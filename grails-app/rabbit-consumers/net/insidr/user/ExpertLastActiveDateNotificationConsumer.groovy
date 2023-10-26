package net.insidr.user

import net.insidr.routing.NotificationConsumer

class ExpertLastActiveDateNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.expertLastActiveDate",
            retry: true,
    ]

    def handle(body) {
        def expert = Expert.findByUserId(body?.user?.id)
        if (expert) {
            expert.lastActiveDate = new Date()
            expert.save()
        } else {
            log.debug "No such expert for last active date notification, userId: ${body?.user?.id}"
        }
    }

}
