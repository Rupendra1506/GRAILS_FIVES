package net.insidr.routing

enum RoutingUnitOfWorkStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELED,
    ERROR,
    CLAIMED,
}
