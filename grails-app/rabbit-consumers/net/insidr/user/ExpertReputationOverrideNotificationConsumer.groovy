package net.insidr.user

import net.insidr.routing.NotificationConsumer

class ExpertReputationOverrideNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.expertReputationOverride",
            retry: true,
    ]

    def handle(body) {
        ExpertReputationOverride.createUpdateOrDeleteFrom(body.reputationOverride)
    }

}

