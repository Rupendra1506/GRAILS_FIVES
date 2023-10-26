package allnew.directly.routing.engine

import net.insidr.internal.status.InternalStatusCode

class HealthController {

    def redisService
    def statusService

    def index() {
        [
                version: grailsApplication.config.directly.version.buildNumber,
                buildNumber: grailsApplication.config.directly.version.buildNumber,
        ]
    }

    def status() {
        def currentStatus = statusService.current()
        if (currentStatus?.status != InternalStatusCode.OK) {
            response.status = 500
        }
        respond currentStatus
    }

}
