package net.insidr.company

import net.insidr.routing.NotificationConsumer
import net.insidr.user.Expert

class AlertRestrictionActivityNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.alertRestrictionActivity",
            retry: true,
    ]

    def handle(body) {
        body.alertRestrictionExpertActivity.each { Expert.updateAlertRestriction it }
    }

}
