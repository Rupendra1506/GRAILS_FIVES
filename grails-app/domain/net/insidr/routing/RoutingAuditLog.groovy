package net.insidr.routing


class RoutingAuditLog implements Serializable {

    long questionId
    long userId

    String routingClassification

    double targetPercentage
    Double routingInstructionPercentage
    Integer routingInstructionDelay

    Date dueDate

    Date dateCreated

    static constraints = {
        routingClassification nullable: true
        targetPercentage nullable: true
        routingInstructionPercentage nullable: true
        routingInstructionDelay nullable: true
        dueDate nullable: true
    }

    static mapping = {
        id composite: [ "questionId", "userId" ]
        version false
    }

    boolean equals(other) {
        if (!(other instanceof RoutingAuditLog)) {
            return false
        }

        other.questionId == questionId &&
                other.userId == userId
    }


}
