package net.insidr.user

import net.insidr.routing.NotificationConsumer

class ExpertUpdateNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.expertUpdate",
            retry: true,
    ]

    def handle(body) {
        def insider = body.insider
        Expert.createOrUpdateFrom(insider)
        def shouldStopRouting = insider.expertHealth == ExpertHealth.BANNED.name() || insider.enabled == false
        if (shouldStopRouting) {
            ExpertReputation.deleteAllByExpertId(insider.id)
        }
    }

}
