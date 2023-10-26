package net.insidr.rabbitmq

import grails.converters.JSON
import org.springframework.transaction.annotation.Transactional

@Transactional
class MessageBusService {

    def grailsApplication
    def rabbitMessagePublisher

    def sendRoutingMessage(userId, questionId, notificationClassName) {
        sendMessage(
                grailsApplication.config.directly.exchanges.routing,
                notificationClassName,
                [
                        insider: [ id: userId ],
                        question: [ id: questionId ],
                        notification: [ class: notificationClassName ],
                ],
        )
    }

    def sendEscalationMessage(questionId, notificationClassName) {
        sendMessage(
                grailsApplication.config.directly.exchanges.escalation,
                notificationClassName,
                [
                        question: [ id: questionId ],
                        notification: [ class: notificationClassName ],
                ],
        )
    }

    def sendMessage(destinationExchange, destinationRoutingKey, message) {
        def augmentedMessage = message + [ origin: grailsApplication.config.directly.origin ]
        def json = augmentedMessage as JSON

        log.info "Publishing ${augmentedMessage} to \"${destinationExchange}\" with routing key \"${destinationRoutingKey}\""

        rabbitMessagePublisher.send {
            exchange = destinationExchange
            routingKey = destinationRoutingKey
            body = json as String
        }

        return augmentedMessage
    }

}
