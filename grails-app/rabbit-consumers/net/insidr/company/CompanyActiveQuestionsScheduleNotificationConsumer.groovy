package net.insidr.company

import net.insidr.routing.NotificationConsumer

class CompanyActiveQuestionsScheduleNotificationConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.companyActiveQuestionsSchedule",
            retry: true,
    ]

    def handle(body) {
        def companySchedule = body.companySchedule
        if(companySchedule){
            CompanyQuestionsSchedule.createOrUpdateFrom(companySchedule)
        }
    }
}
