package net.insidr.routing

class ReleasedQuestionNotificationConsumer extends RoutingStatusChangeNotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.releasedQuestions",
            retry: true,
    ]

    // These are used in handle() method in parent class
    def targetStatuses = [RoutingUnitOfWorkStatus.CLAIMED]
    def newStatus = RoutingUnitOfWorkStatus.PENDING

}

