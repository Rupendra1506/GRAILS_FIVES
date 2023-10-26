package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import net.insidr.user.ExpertReputation
import net.insidr.api.model.ExpertReputationList

@Api(value = "/v1/expert/<expertId>/reputationMetrics?companyTag=<companyTag>", tags = ["Expert"], description = "Expert endpoints", position = 0)
@Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
class ExpertController extends ApiBaseController {
    static namespace = "v1"

    static responseFormats = [ "json" ]

    def springSecurityService

    @ApiOperation(
            value = "Retrieves the expert reputation metrics for a given expert across multiple companies",
            nickname = "/",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "GET",
            response = net.insidr.api.model.ExpertReputationList.class
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "expertId",
                    paramType = "path",
                    required = true,
                    value = "Expert id",
                    dataType = "string",
                    example = "2051"),
            @ApiImplicitParam(name = "Authorization",
                    paramType = "header",
                    required = true,
                    value = "Bearer jwt-token",
                    dataType = "string"),
    ])

    @Secured(["ROLE_ADMIN", "ROLE_OPERATIONS", "ROLE_COMPANY_ADMIN", "ROLE_INSIDER"])
    def showReputationMetrics(ExpertShowReputationMetricsCommand cmd) {
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        def expertReputations = ExpertReputation.findAllByExpertId(cmd.expertId).findAll {
            springSecurityService.principal.hasExpertAccessForCompany(it.company?.alias)
        }

        def expertReputationList = new ExpertReputationList(expertReputations: expertReputations)
        ok(expertReputationList)
    }
}

class ExpertShowReputationMetricsCommand {

    String expertId

    static constraints = {
        expertId nullable: false
    }

}
