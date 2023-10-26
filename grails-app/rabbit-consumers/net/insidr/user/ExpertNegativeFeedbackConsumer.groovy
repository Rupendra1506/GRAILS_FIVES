package net.insidr.user

import net.insidr.routing.NotificationConsumer
import net.insidr.user.groups.NegativeFeedbackExpertGroup

class ExpertNegativeFeedbackConsumer extends NotificationConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.expertNegativeFeedback",
            retry: true,
    ]

    def handle(body) {
      new NegativeFeedbackExpertGroup(body.company?.tagId).add(body.insider?.id)
    }

}
