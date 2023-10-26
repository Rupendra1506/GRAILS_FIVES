package net.insidr.routing

enum RoutingClassification {

    HIGH_CSAT_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 1.0, delay: 0) ]
    ),

    HIGH_CSAT_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 0) ]
    ),

    HIGH_CSAT_LOW_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 0) ]
    ),

    MED_CSAT_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 0), new RoutingRule(percentage: 1.0, delay: 300) ]
    ),

    MED_CSAT_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 0), new RoutingRule(percentage: 0.5, delay: 300) ]
    ),

    MED_CSAT_LOW_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 300) ]
    ),

    LOW_CSAT_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 300) ]
    ),

    LOW_CSAT_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 300) ]
    ),

    LOW_CSAT_LOW_ACTIVE(
            []
    ),

    HIGH_HELPFUL_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 1.0, delay: 0) ]
    ),

    HIGH_HELPFUL_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 0) ]
    ),

    HIGH_HELPFUL_LOW_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 0) ]
    ),

    MED_HELPFUL_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 0), new RoutingRule(percentage: 1.0, delay: 300) ]
    ),

    MED_HELPFUL_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 0), new RoutingRule(percentage: 0.5, delay: 300) ]
    ),

    MED_HELPFUL_LOW_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 300) ]
    ),

    LOW_HELPFUL_HIGH_ACTIVE(
            [ new RoutingRule(percentage: 0.5, delay: 300) ]
    ),

    LOW_HELPFUL_MED_ACTIVE(
            [ new RoutingRule(percentage: 0.25, delay: 300) ]
    ),

    LOW_HELPFUL_LOW_ACTIVE(
            []
    )

    List routingRules

    RoutingClassification(routingRules) {
        this.routingRules = routingRules
    }

}
