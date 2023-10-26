package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import net.insidr.config.CompanyConfig
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

import javax.servlet.http.HttpServletResponse

@Api(value = "/v1/config", tags = ["Config"], description = "Config endpoints", position = 0)
@Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
class ConfigController extends ApiBaseController {
    static namespace = "v1"

    static responseFormats = [ "json" ]

    def springSecurityService
    def configService

    @ApiOperation(
        value = "Retrieves the configurations for a company",
        nickname = "/config",
        produces = "application/json",
        consumes = "application/json",
        httpMethod = "GET",
        response = CompanyConfig.class
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
    def showConfig(ShowConfigCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        def companyConfig = CompanyConfig.findByCompanyTag(cmd.companyTag) ?: {}

        ok(companyConfig)
    }

    @ApiOperation(
            value = "Upserts the configurations for company",
            nickname = "/config",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = CompanyConfig.class
    )
    @ApiImplicitParams([
        @ApiImplicitParam(name = "companyTag",
            paramType = "body",
            required = true,
            value = "companyTag/alias/permalink of the company",
            dataType = "string",
            example = "my-company"),
        @ApiImplicitParam(name = "availabilityExpertsPerQuestionPerHourThreshold",
            paramType = "body",
            required = false,
            value = "Ratio of experts to questions in an hour that must be exceeded for expert availability to be true",
            dataType = "double",
            example = "0.4"),
        @ApiImplicitParam(name = "availabilityConfidenceScoreThreshold",
            paramType = "body",
            required = false,
            value = "Confidence score when the availabilityExpertsPerQuestionPerHourThreshold equals the value specified",
            dataType = "double",
            example = "0.8"),
        @ApiImplicitParam(name = "maxQuestionsPerHourPerExpert",
            paramType = "body",
            required = false,
            value = "Volume of questions that can be handled by a single expert in one hour",
            dataType = "integer",
            example = "30"),
        @ApiImplicitParam(name = "absoluteMetricsEnabled",
            paramType = "body",
            required = false,
            value = "Toggle whether or not the company should use the absolute routing algorithm",
            dataType = "boolean",
            example = "true",
            defaultValue = "false"),
        @ApiImplicitParam(name = "delayBetweenBatchesInSeconds",
                paramType = "body",
                required = false,
                value = "Delay between the routing batches in seconds",
                dataType = "integer",
                example = "120"),
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
    def upsertConfig() {
        def body = request.JSON

        try {
            def cmd = new UpsertConfigCommand(body)

            if (!cmd.validate()) {
                return badRequest(cmd.errors)
            }
        } catch (GroovyCastException ex) {
            return badRequest(ex.message)
        }

        def companyConfig = configService.findOrCreateCompanyConfig(body.companyTag)

        if (body.containsKey("availabilityExpertsPerQuestionPerHourThreshold")) {
            companyConfig.availabilityExpertsPerQuestionPerHourThreshold = body.availabilityExpertsPerQuestionPerHourThreshold
        }

        if (body.containsKey("availabilityConfidenceScoreThreshold")) {
            companyConfig.availabilityConfidenceScoreThreshold = body.availabilityConfidenceScoreThreshold
        }

        if (body.containsKey("maxQuestionsPerHourPerExpert")) {
            companyConfig.maxQuestionsPerHourPerExpert = body.maxQuestionsPerHourPerExpert
        }

        if (body.containsKey("absoluteMetricsEnabled")) {
            companyConfig.absoluteMetricsEnabled = body.absoluteMetricsEnabled
        }

        if (body.containsKey("ratingGraceCount")) {
            companyConfig.ratingGraceCount = body.ratingGraceCount
        }

        if (body.containsKey("delayBetweenBatchesInSeconds")) {
            companyConfig.delayBetweenBatchesInSeconds = body.delayBetweenBatchesInSeconds
        }

        try {
            if (!companyConfig.save(flush: true)) {
                return internalServerError("There was an error trying to save  the availability config for ${body.companyTag}")
            }
        } catch(Exception e) {
            log.debug(e.printStackTrace())
        }

        ok(companyConfig)
    }
}

class ShowConfigCommand {
    String companyTag

    static constraints = {
        companyTag nullable: false
    }
}

class UpsertConfigCommand implements Validateable{
    String companyTag
    Double availabilityExpertsPerQuestionPerHourThreshold
    Double availabilityConfidenceScoreThreshold
    Integer maxQuestionsPerHourPerExpert
    boolean absoluteMetricsEnabled
    Integer ratingGraceCount
    Integer delayBetweenBatchesInSeconds

    static constraints = {
        companyTag nullable: false, blank: false
        availabilityExpertsPerQuestionPerHourThreshold nullable: true
        availabilityConfidenceScoreThreshold nullable: true
        maxQuestionsPerHourPerExpert nullable: true
        absoluteMetricsEnabled nullable: true
        ratingGraceCount nullable: true
        delayBetweenBatchesInSeconds nullable: true
    }
}
