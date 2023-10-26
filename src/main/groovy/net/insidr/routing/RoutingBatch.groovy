package net.insidr.routing

class RoutingBatch {
    def expertIds = []
    String classification
    double percentage
    Integer delay
    Boolean alertRestriction = false

    RoutingBatch(expertIds, classification, percentage, delay=0) {
        this.expertIds = expertIds
        this.classification = classification
        this.percentage = percentage
        this.delay = delay
    }

}
