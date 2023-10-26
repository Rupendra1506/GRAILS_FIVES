import grails.converters.JSON
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils

class BootStrap {

    def init = { servletContext ->
        SpringSecurityUtils.clientRegisterFilter("corsFilter", SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order - 1)
        JSON.registerObjectMarshaller(Enum, { Enum e -> e.name() })
    }

    def destroy = {
    }

}
