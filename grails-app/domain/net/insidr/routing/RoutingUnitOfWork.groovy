package net.insidr.routing

class RoutingUnitOfWork {

    RoutingUnitOfWorkStatus status = RoutingUnitOfWorkStatus.PENDING
    String host
    long questionId
    long userId
    boolean alertRestricted
    Date dueDate = new Date()

    Date dateCreated
    Date lastUpdated

    static constraints = {
        host nullable: true
    }

}
