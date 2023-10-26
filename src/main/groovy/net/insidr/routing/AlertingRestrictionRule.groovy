package net.insidr.routing

class AlertingRestrictionRule {
    static values = [
            [
                    minimumHoursSinceLastActivity: Double.NEGATIVE_INFINITY,
                    maximumHoursSinceLastActivity: 1,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: Double.POSITIVE_INFINITY,
            ],
            [
                    minimumHoursSinceLastActivity: 1,
                    maximumHoursSinceLastActivity: 2,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: 2,
            ],
            [
                    minimumHoursSinceLastActivity: 2,
                    maximumHoursSinceLastActivity: 6,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: 1,
            ],
            [
                    minimumHoursSinceLastActivity: 6,
                    maximumHoursSinceLastActivity: 12,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: 0.5,
            ],
            [
                    minimumHoursSinceLastActivity: 12,
                    maximumHoursSinceLastActivity: 24,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: 0.2,
            ],
            [
                    minimumHoursSinceLastActivity: 24,
                    maximumHoursSinceLastActivity: Double.POSITIVE_INFINITY,
                    minimumActivityLevel: 0.2,
                    maximumActivityLevel: 1,
                    maximumAlertsPerHour: 0.1,
            ],
            [
                    minimumHoursSinceLastActivity: Double.NEGATIVE_INFINITY,
                    maximumHoursSinceLastActivity: 1,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 1,
            ],
            [
                    minimumHoursSinceLastActivity: 1,
                    maximumHoursSinceLastActivity: 2,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 0.1,
            ],
            [
                    minimumHoursSinceLastActivity: 2,
                    maximumHoursSinceLastActivity: 6,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 0.1,
            ],
            [
                    minimumHoursSinceLastActivity: 6,
                    maximumHoursSinceLastActivity: 12,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 0.1,
            ],
            [
                    minimumHoursSinceLastActivity: 12,
                    maximumHoursSinceLastActivity: 24,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 0.1,
            ],
            [
                    minimumHoursSinceLastActivity: 24,
                    maximumHoursSinceLastActivity: Double.POSITIVE_INFINITY,
                    minimumActivityLevel: 0,
                    maximumActivityLevel: 0.2,
                    maximumAlertsPerHour: 1 / 24,
            ],
    ]

}
