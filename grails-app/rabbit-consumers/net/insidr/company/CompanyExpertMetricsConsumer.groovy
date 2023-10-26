package net.insidr.company

import net.insidr.routing.MetricsConsumer
import net.insidr.user.ExpertReputation

class CompanyExpertMetricsConsumer extends MetricsConsumer {

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.metrics.andre.expertMetrics",
            retry: true,
    ]

    def handle(body) {
        def tagId = body.tagId

        CompanyReputation.createFrom(body.companyMetrics, tagId)

        for (expert in body.experts) {
            ExpertReputation.createFrom(expert, tagId)
        }
    }

}
