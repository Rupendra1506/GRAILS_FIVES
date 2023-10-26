package net.insidr.config

class RoutingMetricThresholds {

    CompanyConfig companyConfig
    String metricName
    Double lowThreshold
    Double highThreshold

    boolean enabled = true

    Date dateCreated
    Date lastUpdated
}
