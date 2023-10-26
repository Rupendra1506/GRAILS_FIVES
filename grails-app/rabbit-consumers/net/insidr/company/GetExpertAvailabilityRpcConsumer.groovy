package net.insidr.company

import com.budjb.rabbitmq.consumer.MessageContext

class GetExpertAvailabilityRpcConsumer {

    def grailsApplication
    def companyService

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.rpc.andre.getExpertAvailability",
            retry: true,
    ]

    def handleMessage(body, MessageContext context) {
        def result = [:]
        def companyTag = body?.companyTag
        if (!companyTag) {
           return result.error = [ message: "companyTag param was not defined" ]
        }

        try {
            def metrics = companyService.getExpertAvailabilityMetrics(companyTag, body.certificationTagIds)
            def confidenceScore = metrics.confidenceScore
            def threshold = metrics.threshold
            def isExpertAvailable = confidenceScore >= threshold

            return result.data =  [
                rpcApiResponse: [
                    confidenceScore: formatDecimal(confidenceScore),
                    threshold: formatDecimal(threshold),
                    isExpertAvailable: isExpertAvailable,
                    details: [
                        onlineExpertCount: metrics.onlineExpertCount,
                        questionsPerHour: metrics.questionsPerHour,
                        questionsPerOnlineExpertPerHourThreshold: metrics.questionsPerOnlineExpertPerHourThreshold,
                    ]
                ]
            ]
        } catch (any){
            return result.error = [ exception: any.getClass().name, message: any.message ]
        }
    }

    def formatDecimal(num) {
        return Math.round(num * 100) / 100
    }

}

