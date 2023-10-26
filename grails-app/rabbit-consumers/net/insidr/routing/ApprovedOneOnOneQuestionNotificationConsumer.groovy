package net.insidr.routing

import net.insidr.company.Company
import net.insidr.question.Question
import net.insidr.tag.Tag

class ApprovedOneOnOneQuestionNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.approvedOneOnOneQuestions",
            consumers: 10,
            retry: true,
    ]

    def grailsApplication
    def routingService

    def handle(body) {
        def question = Question.buildQuestionFrom(body.question, body.poster)

        def companyId = getCompanyId(body)
        if (routingService.shouldRouteQuestion(question, companyId)) {
            Company.addQuestionToCompany(question, companyId)
            if (question.useCertificationBasedRouting) {
                question.certifications.each { certification -> Tag.addQuestionToTag(question, certification) }
            }
            log.debug "RoutingService ready to route question with id ${question.questionId}"
            routingService.route(question)
        } else {
            log.debug "${grailsApplication.config.directly.origin}: Question ${question.questionId} discarded for routing."
        }
    }

}
