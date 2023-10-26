package net.insidr.routing

import com.budjb.rabbitmq.consumer.MessageContext
import groovy.json.JsonSlurper
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value

import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat

class NotificationConsumer {

    static final log = LogFactory.getLog(this)

    @Value('${directly.consumers.maxAttempts}')
    int maxAttempts

    def isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    def redisService

    {
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
    }
    
    def getCompanyId(body) {
        body.company?.id
    }

    def handleMessage(String body, MessageContext context) {
        try {
            def slurper = new JsonSlurper()
            def jsonBody = slurper.parseText(body)
            handleMessage(jsonBody, context)
        } catch (any) {
            log.error "Unable parse json from NotificationConsumer ${any}"
        }
    }

    def handleMessage(body, MessageContext context) {
        try {
            handle(body)
        } catch (InvocationTargetException ex) {
            log.error "Caught an invocation target exception"
            def cause = ex.cause
            log.error "Invocation failed as a result of ${cause.message}"
        } catch (any) {
            log.error "Something went wrong in the NotificationConsumer ${any}"
            def errorCount = redisService.incr("${body.notification.class} : ${body.notification.id} error count")
            if (errorCount < maxAttempts) {
                throw any
            } else {
                redisService.sadd("abandoned-notifications", [ "${body.notification.class} : ${body.notification.id}" ] as String[])
                redisService.sadd("${body.notification.class} : ${body.notification.id} attempts", [ isoFormat.format(new Date()) ] as String[])
            }
        }
    }

}
