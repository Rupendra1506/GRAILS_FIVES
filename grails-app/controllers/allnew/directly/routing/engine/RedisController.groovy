package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import io.swagger.annotations.*
import net.insidr.company.Company
import net.insidr.user.MetricTiers
import net.insidr.util.RedisKeys

import javax.servlet.http.HttpServletResponse

@Api(value = "/v1/redis", tags = ["Redis"], description = "Redis endpoints", position = 0)
@Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
class RedisController extends ApiBaseController {
    static namespace = "v1"

    def springSecurityService
    def redisService

    @ApiOperation(
            value = "Clear a given redis key from the database",
            nickname = "/redis/clearKey",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = String
    )
    @ApiImplicitParams([
        @ApiImplicitParam(name = "key",
                paramType = "body",
                required = true,
                value = "Key which to clear from the Redis database",
                dataType = "string",
                example = "net.insidr.user.ExpertReputation:expertId:119:reputations"),
        @ApiImplicitParam(name = "Authorization",
            paramType = "header",
            required = true,
            value = "Bearer jwt-token",
            dataType = "string"),
    ])
    @ApiResponses([
        @ApiResponse(code = HttpServletResponse.SC_OK,
            message = "Successfully deleted"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST,
            message = "Invalid arguments. Find more details in the response."),
        @ApiResponse(code = HttpServletResponse.SC_UNAUTHORIZED,
            message = "Invalid Authorization Credentials"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN,
            message = "Forbidden. Current user does not have permission to perform that operation"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            message = "Internal server error.")
    ])

    @Secured(["ROLE_ADMIN"])
    def clearRedisKey(ClearRedisKeyCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        if (redisService.exists(cmd.key)) {
            redisService.del(cmd.key)
        } else {
            return badRequest("${cmd.key} does not exist")
        }

        success()
    }

    @ApiOperation(
            value = "Clear a list of questions for a company from the database",
            nickname = "/redis/clearQuestions",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = String
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
                    message = "Successfully deleted"),
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
    def clearQuestionsForCompany(ClearCompanySpecificKeyCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        if (!springSecurityService.principal.hasExpertAccessForCompany(cmd.companyTag)) {
            return notAuthorized("You do not have access to company: ${cmd.companyTag}")
        }

        def companyId = Company.findByAlias(cmd.companyTag)?.companyId

        def companyQuestionsKey = RedisKeys.buildCompanyQuestionsKey(companyId)
        if (redisService.exists(companyQuestionsKey)) {
            redisService.del(companyQuestionsKey)
        } else {
            return badRequest("Entry does not exist for company ${cmd.companyTag}")
        }

        success()
    }

    @ApiOperation(
            value = "Clear stored percentOfQuestionsPerExpert value for a company from the database",
            nickname = "/redis/clearPercentOfQuestionsPerExpert",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = String
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
                    message = "Successfully deleted"),
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
    def clearPercentPerExpertsPerQuestionForCompany(ClearCompanySpecificKeyCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        if (!springSecurityService.principal.hasExpertAccessForCompany(cmd.companyTag)) {
            return notAuthorized("You do not have access to company: ${cmd.companyTag}")
        }

        def companyId = Company.findByAlias(cmd.companyTag)?.companyId

        def companyPercentOfExpertsPerQuestionKey = RedisKeys.buildCompanyPercentOfExpertsPerQuestionKey(companyId)
        if (redisService.exists(companyPercentOfExpertsPerQuestionKey)) {
            redisService.del(companyPercentOfExpertsPerQuestionKey)
        } else {
            return badRequest("Entry does not exist for company ${cmd.companyTag}")
        }

        success()
    }

    @ApiOperation(
            value = "Clear all expert routing overrides for a company from the database",
            nickname = "/redis/clearOverrides",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = String
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
                    message = "Successfully deleted"),
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
    def clearOverridesForCompany(ClearCompanySpecificKeyCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        if (!springSecurityService.principal.hasExpertAccessForCompany(cmd.companyTag)) {
            return notAuthorized("You do not have access to company: ${cmd.companyTag}")
        }

        def companyTagId = Company.findByAlias(cmd.companyTag)?.tagId
        if (!companyTagId) {
            return badRequest("Entry does not exist for company ${cmd.companyTag}")
        }

        MetricTiers.getAllTiers().each {
            redisService.del(RedisKeys.buildTierOverrideAdditionsKey(it, companyTagId))
        }

        success()
    }

    @ApiOperation(
            value = "Clear all expert group tiers for a company from the database",
            nickname = "/redis/clearTiers",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = String
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
                    message = "Successfully deleted"),
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
    def clearTiersForCompany(ClearCompanySpecificKeyCommand cmd){
        if (!cmd.validate()){
            return badRequest(cmd.errors)
        }
        if (!springSecurityService.principal.hasExpertAccessForCompany(cmd.companyTag)) {
            return notAuthorized("You do not have access to company: ${cmd.companyTag}")
        }
        def companyTagId = Company.findByAlias(cmd.companyTag)?.tagId
        if (!companyTagId) {
            return badRequest("Entry does not exist for company ${cmd.companyTag}")
        }
        MetricTiers.getAllTiers().each {
            redisService.del(RedisKeys.buildTierKey(it, companyTagId))
        }
        success()

    }
}

class ClearRedisKeyCommand {
    String key

    static constraints = {
        key nullable: false, blank: false
    }
}

class ClearCompanySpecificKeyCommand {
    String companyTag

    static constraints = {
        companyTag nullable: false, blank: false
    }
}
