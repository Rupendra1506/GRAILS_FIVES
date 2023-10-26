package allnew.directly.routing.engine

import grails.plugin.springsecurity.annotation.Secured
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import net.insidr.routing.QuestionRoutingSummary
import net.insidr.routing.RoutingUnitOfWork

import javax.servlet.http.HttpServletResponse

@Api(value = "/v1/", tags = ["RoutingUnitOfWork"], description = "Routing Units Of Work", position = 0)
@Secured(["ROLE_ADMIN", "ROLE_OPERATIONS"])
class RoutingUnitOfWorkController extends ApiBaseController {
    static namespace = "v1"

    static responseFormats = [ "json" ]

    def springSecurityService

    @ApiOperation(
            value = "Retrieves the routing unit of work for a question",
            nickname = "/routingUnitOfWork",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "GET",
            response = RoutingUnitOfWork.class
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "questionId",
                    paramType = "query",
                    required = true,
                    value = "ID of the question",
                    dataType = "integer",
                    example = "2134"),
            @ApiImplicitParam(name = "Authorization",
                    paramType = "header",
                    required = true,
                    value = "Bearer jwt-token",
                    dataType = "string"),
    ])
    @ApiResponses([
            @ApiResponse(code = HttpServletResponse.SC_OK,
                    message = "Return the unit of work json having userId, dueDate and status.",
                    response = RoutingUnitOfWork.class),
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
    def showRoutingUnitOfWork(ShowRoutingUnitOfWorkCommand cmd){
        if (!cmd.validate()) {
            return badRequest(cmd.errors)
        }

        def unitsOfWork = [] as List

        RoutingUnitOfWork.where {
            questionId == cmd.questionId
        }.list().each{it->
            def map = [user_id:it.userId, due_date: it.dueDate, status: it.status]
            unitsOfWork += map}

        def summaries = QuestionRoutingSummary.where {
            questionId == cmd.questionId
        }.list()

        if (unitsOfWork.size() == 0) {
            return notFound("Unit of work not found for question ID ${cmd.questionId}.")
        }

        if (!springSecurityService.principal.hasExpertAccessForCompany(summaries[0].companyTag)) {
            return notAuthorized("You do not have access to question ID ${cmd.questionId}")
        }
        ok(unitsOfWork)
    }
}

class ShowRoutingUnitOfWorkCommand {
    String questionId

    static constraints = {
        questionId nullable: false
    }
}
