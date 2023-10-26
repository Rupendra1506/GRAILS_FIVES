package net.insidr.routing

import net.insidr.util.SQLUtil

class RoutingStatusChangeNotificationConsumer extends NotificationConsumer {

    def routingService
    def dataSource

    // These variables should be overridden in implementation classes
    def targetStatuses
    def newStatus

    def handle(body) {
        def question = body.question
        def questionId = question?.id

        if (!routingService.shouldHandleQuestion(body.company?.id)) {
            log.debug "Question ${questionId} discarded for release."
            return
        }

        // getNewStatus() will get newStatus defined in child implementations of class
        RoutingUnitOfWork.withTransaction {
            try {
                if (SQLUtil.acquireRoutingLock(dataSource)) {
                    def routingUnitsOfWork = findRoutingUnitsOfWork(questionId)
                    routingUnitsOfWork.each { routingService.markRoutingUnitOfWork(it, getNewStatus()) }
                } else {
                    log.info "Could not get routing lock"
                }
            } catch (any) {
                log.error "Unable to mark RoutingUnitOfWork for ${questionId} as ${getNewStatus()}", any
            } finally {
                SQLUtil.releaseRoutingLock(dataSource)
            }
        }

        executeAfterUpdate(body)
    }

    // getTargetStatuses() will get targetStatuses defined in child implementations of class
    def findRoutingUnitsOfWork(questionId) {
        return RoutingUnitOfWork.findAllByQuestionIdAndStatusInList(questionId, getTargetStatuses())
    }

    def executeAfterUpdate(body) { }

}

