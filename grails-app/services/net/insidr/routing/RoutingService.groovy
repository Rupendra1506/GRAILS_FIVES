package net.insidr.routing

import org.springframework.transaction.annotation.Transactional
import net.insidr.company.Company
import org.joda.time.DateTime
import org.springframework.dao.DuplicateKeyException

import java.text.MessageFormat

@Transactional
class RoutingService {

    def messageBusService
    def routingClassificationService
    def grailsApplication

    def random = new Random()

    def route(question) {
        def routingInstructions = routingClassificationService.determineRoutingInstructions(question, question.companyId)
        executeRoutingInstructions(question, routingInstructions)
    }

    def escalate(question) {
        def routingInstructions = routingClassificationService.getEscalationRoutingInstructions(question, question.companyId)
        if (routingInstructions) {
            executeRoutingInstructions(question, routingInstructions)
        } else {
            log.warn "No official experts found for company ${question.companyId}"
            return
        }
    }

    def executeRoutingInstructions(question, routingInstructions) {
        createQuestionRoutingSummary(routingInstructions)
        for (routingBatch in routingInstructions.routingPlan) {
            if (routingBatch) {
                def questionDueDate = new DateTime().plusSeconds(question.routingDelayInSeconds).plusSeconds(routingBatch.delay)
                routeQuestionToBatch(question, routingBatch, routingInstructions.targetPercentage, questionDueDate)
            }
        }

        if (!existsInAuditLog(question)) {
            log.error "Question not found in audit log, possibly not enough experts to route to for question ${question.questionId} for company ${question.companyId}"
            messageBusService.sendEscalationMessage(question.questionId, "net.insidr.question.EscalatedQuestionByLackOfExpertsNotification")
        }
    }

    def routeQuestionToBatch(question, routingBatch, targetPercentage, dueDate) {
        for (expertId in routingBatch.expertIds) {
            log.debug("Expert IDs : ${routingBatch.expertIds}")
            // To handle legacy official  expert records, which were previously concatenated to companyTagIds, e.g. "83717_56"
            if (expertId.contains("_")) {
                continue
            }
            if (question.userId == expertId) {
                log.info "Not routing question to expert ${expertId}, as expert was the poster for question ${question.questionId}"
                messageBusService.sendRoutingMessage(expertId, question.questionId, "net.insidr.routing.DoNotRouteQuestionToExpertNotification")
                return
            }
            if (existsInAuditLog(question, expertId)) {
                log.info "Not routing question to expert ${expertId}, as expert has already been routed question ${question.questionId}"
                return
            }

            createAuditLogEntry(question, expertId, routingBatch, targetPercentage, dueDate)
            createUnitOfWork(question, expertId, dueDate, routingBatch.alertRestriction)
        }
    }

    def existsInAuditLog(question, expertId) {
        return RoutingAuditLog.where { questionId == question.questionId && userId == expertId as Long }.count() != 0
    }

    def existsInAuditLog(question) {
        return RoutingAuditLog.where { questionId == question.questionId }.count() != 0
    }

    def createQuestionRoutingSummary(RoutingInstructions routingInstructions) {
        def questionRoutingSummary = new QuestionRoutingSummary(routingInstructions)
        if (!questionRoutingSummary.save(flush: true)) {
            log.error "Cannot create ${questionRoutingSummary} for question: ${routingInstructions.question.questionId}: ${questionRoutingSummary.errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", ")}"
        }
    }

    def createAuditLogEntry(question, expertId, routingBatch, targetPercentage, dueDate) {
        try {
            new RoutingAuditLog(
                    questionId: question.questionId,
                    userId: expertId,
                    routingClassification: routingBatch.classification,
                    targetPercentage: targetPercentage,
                    routingInstructionPercentage: routingBatch.percentage,
                    routingInstructionDelay: routingBatch.delay,
                    dueDate: dueDate,
            ).save()
        } catch (DuplicateKeyException ex) {
            // Swallow this exception, most likely due to nested NonUniqueObjectException
            // that we can safely ignore.
        } catch (any) {
            log.error "There was a problem logging the attempt to route question ${question.questionId} to expert ${expertId}: ${any.message}", any
        }
    }

    def createUnitOfWork(question, expertId, dueDate, alertRestriction) {
        def routingUnitOfWork = new RoutingUnitOfWork(
                questionId: question.questionId,
                userId: expertId,
                dueDate: dueDate,
                alertRestricted: alertRestriction,
        )
        if (!routingUnitOfWork.save(flush: true)) {
            log.error "Cannot create ${routingUnitOfWork} for ${question} to ${expertId}: ${routingUnitOfWork.errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", ")}"
        }
    }

    def shouldHandleQuestion(companyId) {
        Company.getRoutingEngine(companyId) == grailsApplication.config.directly.origin
    }

    def shouldRouteQuestion(question, companyId) {
        return shouldHandleQuestion(companyId) && !existsInAuditLog(question)
    }

    def markRoutingUnitOfWork(routingUnitOfWork, status) {
        def host = System.getProperty("insidr.host") ?: InetAddress.localHost.hostName
        routingUnitOfWork.status = status
        routingUnitOfWork.host = host
        if (routingUnitOfWork.save(flush: true)) {
            return true
        } else {
            def message = "Cannot update ${routingUnitOfWork}: ${routingUnitOfWork.errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", ")}"
            log.error message
            return false
        }
    }

}
