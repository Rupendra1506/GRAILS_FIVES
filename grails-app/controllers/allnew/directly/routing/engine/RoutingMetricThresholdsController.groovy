package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import io.swagger.annotations.*
import net.insidr.config.CompanyConfig
import net.insidr.config.RoutingMetricThresholds

import javax.servlet.http.HttpServletResponse

@Api(value = "/v1/thresholds", tags = ["Thresholds"], description = "Routing Metric Thresholds endpoints", position = 0)
@Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
class RoutingMetricThresholdsController extends ApiBaseController {
    static namespace = "v1"

    static responseFormats = [ "json" ]

    def springSecurityService
    def configService

    @ApiOperation(
        value = "Retrieves the metric thresholds for a company",
        nickname = "/thresholds",
        produces = "application/json",
        consumes = "application/json",
        httpMethod = "GET",
        response = RoutingMetricThresholds.class,
        responseContainer = "List"
    )
    @ApiImplicitParams([
        @ApiImplicitParam(name = "companyTag",
            paramType = "query",
            required = true,
            value = "companyTag/alias/permalink of the company",
            dataType = "string",
            example = "my-company"),
        @ApiImplicitParam(name = "Authorization",
            paramType = "header",
            required = true,
            value = "Bearer jwt-token",
            dataType = "string"),
    ])
    @ApiResponses([
        @ApiResponse(code = HttpServletResponse.SC_OK,
            message = "Return the client config json in the data section.",
            response = CompanyConfig.class),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST,
            message = "Invalid arguments. Find more details in the response."),
        @ApiResponse(code = HttpServletResponse.SC_UNAUTHORIZED,
            message = "Invalid Authorization Credentials"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN,
            message = "Forbidden. Current user does not have permission to perform that operation"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND,
            message = "Not Found"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            message = "Internal server error.")
    ])

    @Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
    def showRoutingMetricThresholds(ShowRoutingMetricThresholdsCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        def companyConfig = CompanyConfig.findByCompanyTag(cmd.companyTag)
        def thresholds = []
        if (companyConfig) {
            thresholds = RoutingMetricThresholds.findAllByCompanyConfig(companyConfig)
        }

        ok([
                companyTag: cmd.companyTag,
                thresholds: thresholds
        ])
    }

    @ApiOperation(
            value = "Upserts routing metric thresholds for a company",
            nickname = "/thresholds",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = RoutingMetricThresholds.class
    )
    @ApiImplicitParams([
        @ApiImplicitParam(name = "companyTag",
            paramType = "body",
            required = true,
            value = "companyTag/alias/permalink of the company",
            dataType = "string",
            example = "my-company"),
        @ApiImplicitParam(name = "metricName",
            paramType = "body",
            required = true,
            value = "The name of the metric for which the thresholds are being set",
            dataType = "string",
            example = "csat"),
        @ApiImplicitParam(name = "lowThreshold",
            paramType = "body",
            required = true,
            value = "The threshold for determining STOPPED or REGULAR routing status",
            dataType = "double",
            example = "0.4"),
        @ApiImplicitParam(name = "highThreshold",
            paramType = "body",
            required = true,
            value = "The threshold for determining REGULAR or PREFERRED routing status",
            dataType = "double",
            example = "0.8"),
        @ApiImplicitParam(name = "enabled",
            paramType = "body",
            required = false,
            value = "Toggle whether or not the company should use this routing metric threshold",
            dataType = "boolean",
            example = "true",
            defaultValue = "false"),
        @ApiImplicitParam(name = "Authorization",
            paramType = "header",
            required = true,
            value = "Bearer jwt-token",
            dataType = "string"),
    ])
    @ApiResponses([
        @ApiResponse(code = HttpServletResponse.SC_OK,
            message = "Return the client config json in the data section.",
            response = CompanyConfig.class),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST,
            message = "Invalid arguments. Find more details in the response."),
        @ApiResponse(code = HttpServletResponse.SC_UNAUTHORIZED,
            message = "Invalid Authorization Credentials"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN,
            message = "Forbidden. Current user does not have permission to perform that operation"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            message = "Internal server error.")
    ])

    @Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
    def upsertRoutingMetricThresholds(UpsertRoutingMetricThresholdsCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        def companyConfig = configService.findOrCreateCompanyConfig(cmd.companyTag)

        def thresholds = RoutingMetricThresholds.findByCompanyConfigAndMetricName(companyConfig, cmd.metricName)
        if (!thresholds) {
            thresholds = new RoutingMetricThresholds(
                    companyConfig: companyConfig,
                    metricName: cmd.metricName,
            )
        }

        thresholds.lowThreshold = cmd.lowThreshold
        thresholds.highThreshold = cmd.highThreshold
        thresholds.enabled = cmd.enabled

        if (!thresholds.save(flush: true)) {
            return internalServerError("There was an error trying to save the routing metric threshold for ${cmd.companyTag}")
        }

        ok(thresholds)
    }
}

class ShowRoutingMetricThresholdsCommand {
    String companyTag

    static constraints = {
        companyTag nullable: false
    }
}

class UpsertRoutingMetricThresholdsCommand {
    String companyTag
    String metricName
    Double lowThreshold
    Double highThreshold
    boolean enabled

    static constraints = {
        companyTag nullable: false, blank: false
        metricName nullable: false, blank: false
        lowThreshold nullable: false
        highThreshold nullable: false
        enabled nullable: false
    }
}
