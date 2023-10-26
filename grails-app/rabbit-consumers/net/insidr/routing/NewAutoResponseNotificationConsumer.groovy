package net.insidr.routing

class NewAutoResponseNotificationConsumer extends RoutingStatusChangeNotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.newAutoResponse",
            retry: true,
    ]

    // Used in handle() method in parent class
    def newStatus = RoutingUnitOfWorkStatus.CLAIMED

    @Override
    def findRoutingUnitsOfWork(questionId) {
        def numCompletedUnitsOfWork = RoutingUnitOfWork.countByQuestionIdAndStatus(questionId, RoutingUnitOfWorkStatus.DONE)
        def pendingUnitsOfWork = RoutingUnitOfWork.findAllByQuestionIdAndStatus(questionId, RoutingUnitOfWorkStatus.PENDING, [sort: "dueDate"])

        if (numCompletedUnitsOfWork > 0) {
            return pendingUnitsOfWork
        }

        if (pendingUnitsOfWork.size() == 0) {
            return []
        }

        def firstDueDate = pendingUnitsOfWork[0].dueDate
        return pendingUnitsOfWork.findAll { it.dueDate != firstDueDate }
    }

}

