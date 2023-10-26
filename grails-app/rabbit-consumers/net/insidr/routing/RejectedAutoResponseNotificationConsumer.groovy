package net.insidr.routing

class RejectedAutoResponseNotificationConsumer extends RoutingStatusChangeNotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
        queue: "directly.notifications.andre.rejectedAutoResponses",
        retry: true,
    ]

    // These are used in handle() method in parent class
    def targetStatuses = [RoutingUnitOfWorkStatus.CLAIMED]
    def newStatus = RoutingUnitOfWorkStatus.PENDING

}

