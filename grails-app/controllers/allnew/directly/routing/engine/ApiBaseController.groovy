package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import org.grails.web.databinding.bindingsource.InvalidRequestBodyException

import java.text.MessageFormat

@Secured("ROLE_ADMIN")
class ApiBaseController {

    static responseFormats = [ 'json' ]

    def success() {
        def data = [ result: "success" ]
        ok(data)
    }

    def failure() {
        badRequest("This action always fails, for testing purposes.")
    }

    def ok(data) {
        log.info "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Succesful call."
        response.status = 200
        respond data
    }

    def noContent() {
        log.info "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Succesful call."
        response.status = 204
        respond [:]
    }

    def notModified() {
        response.status = 304
    }

    def created(data) {
        log.info "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Resource created."
        response.status = 201
        respond data
    }

    protected def badRequest(ValidationErrors errors) {
        badRequest(errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", "))
    }

    protected def badRequest(message) {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Bad request: ${message} "
        response.status = 400
        render view: '/errors/apiError', model: [ message: message ], contentType: "application/json"
    }

    def notAuthenticated(message) {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Not authenticated: ${message} "
        response.status = 401
        render view: '/errors/apiError', model: [ message: message ]
    }

    def notAuthorized(message) {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Not authorized: ${message} "
        response.status = 403
        render view: '/errors/apiError', model: [ message: message ]
    }

    def notFound(message) {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Not found: ${message} "
        response.status = 404
        render view: '/errors/apiError', model: [ message: message ]
    }

    def methodNotAllowed() {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Method not allowed"
        response.status = 405
        render view: '/errors/apiError', model: [ message: "Method '$request.method' not allowed in this endpoint" ]
    }

    def internalServerError(message) {
        log.error "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Internal Server Error: ${message}"
        response.status = 500
        render view: '/errors/apiError', model: [ message: message ]
    }

    def notImplemented() {
        log.warn "[request: ${request.tracker?.id}; URI: ${request.requestURI}] Not Implemented."
        response.status = 501
        render view: '/errors/apiError', model: [ message: "This feature is not implemented yet." ]
    }

    def handleInvalidRequestBodyException(InvalidRequestBodyException e) {
        badRequest("Invalid request body: ${e.cause?.message}")
    }

}
