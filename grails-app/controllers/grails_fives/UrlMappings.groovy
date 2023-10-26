package grails_fives

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/$namespace/expert/$expertId/reputationMetrics" {
            controller = "expert"
            action = "showReputationMetrics"
        }

        get "/$namespace/config/availability?" {
            controller = "config"
            action = "showConfig"
        }

        put "/$namespace/config/availability" {
            controller = "config"
            action = "upsertConfig"
        }

        get "/$namespace/config" {
            controller = "config"
            action = "showConfig"
        }

        put "/$namespace/config" {
            controller = "config"
            action = "upsertConfig"
        }

        get "/$namespace/thresholds" {
            controller = "routingMetricThresholds"
            action = "showRoutingMetricThresholds"
        }

        put "/$namespace/thresholds" {
            controller = "routingMetricThresholds"
            action = "upsertRoutingMetricThresholds"
        }

        get "/$namespace/questionRoutingSummary?" {
            controller = "questionRoutingSummary"
            action = "show"
        }

        get "/$namespace/routingUnitOfWork?" {
            controller = "routingUnitOfWork"
            action = "showRoutingUnitOfWork"
        }

        put "/$namespace/redis/clearKey" {
            controller = "redis"
            action = "clearRedisKey"
        }

        put "/$namespace/redis/clearQuestions" {
            controller = "redis"
            action = "clearQuestionsForCompany"
        }

        put "/$namespace/redis/clearPercentOfExpertsPerQuestion" {
            controller = "redis"
            action = "clearPercentPerExpertsPerQuestionForCompany"
        }

        put "/$namespace/redis/clearOverrides" {
            controller = "redis"
            action = "clearOverridesForCompany"
        }

        put "/$namespace/redis/clearTiers" {
            controller = "redis"
            action = "clearTiersForCompany"
        }

        "/api/docs/$action?/$id?"(controller: "apiDoc", action: "getDocuments")

        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
