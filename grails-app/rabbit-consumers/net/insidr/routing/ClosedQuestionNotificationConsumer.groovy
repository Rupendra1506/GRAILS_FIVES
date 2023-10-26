package net.insidr.routing

class ClosedQuestionNotificationConsumer extends RoutingStatusChangeNotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.closedQuestions",
            consumers: 4,
            retry: true,
    ]

    // These are used in handle() method in parent class
    def targetStatuses = [RoutingUnitOfWorkStatus.PENDING, RoutingUnitOfWorkStatus.CLAIMED]
    def newStatus = RoutingUnitOfWorkStatus.CANCELED

}
