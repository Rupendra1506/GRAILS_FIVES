package net.insidr.company

import net.insidr.routing.NotificationConsumer
import net.insidr.user.Expert
import net.insidr.user.ExpertReputationOverride

class CompanyInitializationAndreNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.companyInitialization",
            retry: true,
    ]

    def handle(body) {
        def company = Company.createOrUpdateFrom(body.company)

        body.company.experts.each { expert ->
            try {
                Expert.createOrUpdateFrom expert
            } catch (any) {
                log.error "Error creating or updating ${expert}", any
                throw any
            }
            
            expert.reputationOverrides.each { override ->
                ExpertReputationOverride.createUpdateOrDeleteFrom(override)
            }
        }

        if (company.alertRestriction) {
            body.alertRestrictionExpertActivity.each { alertRestrictionExpertActivity ->
                try {
                    Expert.updateAlertRestriction alertRestrictionExpertActivity
                } catch (any) {
                    log.error "Error adusting alerting restrictions data for ${alertRestrictionExpertActivity}", any
                    throw any
                }
            }
        }
    }

}
