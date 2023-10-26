package net.insidr.user

enum MetricTiers {
    TIER_1, TIER_2, TIER_3, NA

    List<MetricTiers> getOtherTiers() {
        values().findAll { value -> value.name() != name() }
    }

    static List<MetricTiers> getAllTiers() {
        values()
    }
}

