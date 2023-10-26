package net.insidr.company

import net.insidr.routing.NotificationConsumer

class CompanyUpdateNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.companyUpdate",
            retry: true,
    ]

    def handle(body) {
        Company.createOrUpdateFrom(body.company)
    }

}
