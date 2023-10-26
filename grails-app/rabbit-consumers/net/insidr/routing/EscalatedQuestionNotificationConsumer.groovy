package net.insidr.routing

import net.insidr.question.Question

class EscalatedQuestionNotificationConsumer extends RoutingStatusChangeNotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
        queue: "directly.notifications.andre.escalatedQuestions",
        retry: true,
    ]

    // These are used in handle() method in parent class
    def targetStatuses = [RoutingUnitOfWorkStatus.PENDING, RoutingUnitOfWorkStatus.CLAIMED]
    def newStatus = RoutingUnitOfWorkStatus.CANCELED

    @Override
    def executeAfterUpdate(body) {
        def question = Question.buildQuestionFrom(body.question, body.poster)
        routingService.escalate(question)
    }
}

