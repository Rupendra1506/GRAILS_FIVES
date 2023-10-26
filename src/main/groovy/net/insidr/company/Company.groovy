package net.insidr.company

import grails.util.Holders
import net.insidr.config.CompanyConfig
import net.insidr.user.MetricTiers
import net.insidr.user.OnlineStatus
import net.insidr.user.UserTagAssociation
import net.insidr.user.groups.TierOnlineStatusExpertGroup
import org.apache.commons.logging.LogFactory
import net.insidr.util.RedisKeys
import net.insidr.util.RedisUtil

class Company {

    static final log = LogFactory.getLog(this)
    static final EPOCH_START_TIME = 0
    static final MAX_EXPERT_NEGATIVE_FEEDBACK_HOURS = 24

    def redisService

    def internalMap = [:]

    Company(params = [:]) {
        redisService = RedisUtil.redisService
        internalMap << params
    }

    def getAlertRestriction() { Boolean.valueOf(internalMap.alertRestriction) }
    def setAlertRestriction(alertRestriction) { internalMap.alertRestriction = alertRestriction as String }

    def getCompanyId() { internalMap.companyId as long }
    def getUserId() { internalMap.userId as long }
    def getTagId() { internalMap.tagId as long }
    def getName() { internalMap.name }
    def getAlias() { internalMap.alias }
    def getRoutingEngine() { internalMap.routingEngine }

    def getCompanyReputation() { CompanyReputation.findByTagId(tagId) }

    static createOrUpdateFrom(json) {
        def routingEngine = json.routingEngine?.name ?: Holders.grailsApplication.config.directly.origin
        def company = findByCompanyId(json.id)
        if (company) {
            log.info "Update existing company ${company} for ${json.id} ..."

            company.internalMap.name = json.name
            company.internalMap.alias = json.alias
            company.internalMap.previousName=json.previousName;
            company.internalMap.alertRestriction = json.alertRestriction as String
            company.internalMap.tagId = json.tagId as String
            company.internalMap.routingEngine = routingEngine
            company.save()
        } else {
            log.info "Creating new company ${company} for ${json.id} ..."

            company = new Company(
                    companyId: json.id as String,
                    name: json.name,
                    alias: json.alias,
                    previousName:json.previousName,
                    alertRestriction: json.alertRestriction as String,
                    tagId: json.tagId as String,
                    routingEngine: routingEngine
            ).save()
        }

        return company
    }

    def save() {
        redisService.sadd(RedisKeys.allCompaniesKey, internalMap.companyId)
        redisService.hmset("${Company.name}:${internalMap.companyId}", internalMap)
        redisService.sadd("${Company.name}:tagId:${internalMap.tagId}", internalMap.companyId)
            redisService.del(RedisKeys.buildCompanyAliasCompanyIdKey(internalMap.previousName.toLowerCase()));
        redisService.set(RedisKeys.buildCompanyAliasCompanyIdKey(internalMap.alias), internalMap.companyId)

        return this
    }

    static def findByCompanyId(companyId) {
        def company = new Company()
        company.loadMapByCompanyId(companyId)
    }

    static def findByAlias(alias) {
        def company = new Company()
        company.loadMapByCompanyAlias(alias)
    }

    static def findByTagId(tagId) {
        def company = new Company()
        company.loadMapByTagId(tagId)
    }

    static def getRoutingEngine(companyId) {
        def routingEngine = findByCompanyId(companyId)?.routingEngine
        if (!routingEngine) {
            log.error("Routing Engine information for ${companyId} not found")
        }
        routingEngine
    }

    def loadMapByTagId(tagId) {
        def companyId = redisService.smembers("${Company.name}:tagId:${tagId}")
        loadMapByCompanyId(companyId.first())
    }

    def loadMapByCompanyAlias(alias) {
        def companyId = redisService.get(RedisKeys.buildCompanyAliasCompanyIdKey(alias))

        internalMap << redisService.hgetAll("${Company.name}:${companyId}")
        if (internalMap.isEmpty()) {
            return null
        }

        return this
    }

    def loadMapByCompanyId(companyId) {
        internalMap << redisService.hgetAll("${Company.name}:${companyId}")
        if (internalMap.isEmpty()) {
            return null
        }

        return this
    }

    static addQuestionToCompany(question, companyId) {
        new Company().addQuestionByCompanyId(question, companyId)
    }

    def addQuestionByCompanyId(question, companyId) {
        redisService.zadd(RedisKeys.buildCompanyQuestionsKey(companyId), question.dateCreated.time as Double, question.questionId as String)
    }

    def getQuestionCount(lookbackInMinutes=15) {
        def currentTime = new Date().time
        def lookbackTime = currentTime - (lookbackInMinutes * 60 * 1000)
        return redisService.zcount(RedisKeys.buildCompanyQuestionsKey(companyId), lookbackTime, currentTime)
    }

    def getQuestionsWithCertificationsCount(certificationTagIds, lookbackInMinutes=15) {
        def currentTime = new Date().time
        def lookbackTime = currentTime - (lookbackInMinutes * 60 * 1000)
        def questionIds = redisService.zrangeByScore(RedisKeys.buildCompanyQuestionsKey(companyId), lookbackTime, currentTime) as Set
        if (certificationTagIds) {
            def certificationQuestionIds = certificationTagIds.collect { redisService.zrangeByScore(RedisKeys.buildTagQuestionsKey(it), lookbackTime, currentTime) }.flatten() as Set
            questionIds = questionIds.intersect(certificationQuestionIds)
        }
        return questionIds.size()
    }

    def pruneQuestionEntries(lookbackInMinutes=15) {
        def endTime = new Date().time - (lookbackInMinutes * 60 * 1000)
        redisService.zremrangeByScore(RedisKeys.buildCompanyQuestionsKey(companyId), EPOCH_START_TIME, endTime)
    }

    def getOnlineExpertsWithCertificationsCount(certificationTagIds) {
        def onlineTier1ExpertIds = new TierOnlineStatusExpertGroup(OnlineStatus.online, MetricTiers.TIER_1, internalMap.tagId).experts() as Set
        def onlineTier2ExpertIds = new TierOnlineStatusExpertGroup(OnlineStatus.online, MetricTiers.TIER_2, internalMap.tagId).experts() as Set
        def expertIds = onlineTier1ExpertIds.plus(onlineTier2ExpertIds)
        if (certificationTagIds) {
            def certificationExpertIds = certificationTagIds.collect { redisService.smembers("${UserTagAssociation.name}:tagId:${it}:unofficial") }.flatten() as Set
            expertIds = expertIds.intersect(certificationExpertIds)
        }

        return expertIds.size()
    }

    def getExpertsPerQuestionsPerHour(certificationTagIds) {
        def questionsPerHour = getQuestionsWithCertificationsCount(certificationTagIds, 15) * 4
        def onlineUserCount = getOnlineExpertsWithCertificationsCount(certificationTagIds)
        return questionsPerHour ? onlineUserCount / questionsPerHour : onlineUserCount
    }

    def getAvailabilityConfig() {
        def companyConfig = CompanyConfig.findByCompanyTag(alias)
        def defaultExpertsPerQuestionPerHourThreshold = Holders.grailsApplication.config.availability.defaultExpertsPerQuestionPerHourThreshold
        def defaultConfidenceScoreThreshold = Holders.grailsApplication.config.availability.defaultConfidenceScoreThreshold
        def expertsPerQuestionPerHourThreshold = companyConfig?.availabilityExpertsPerQuestionPerHourThreshold ?: defaultExpertsPerQuestionPerHourThreshold
        def confidenceScoreThreshold = companyConfig?.availabilityConfidenceScoreThreshold ?: defaultConfidenceScoreThreshold

        return [
            availabilityExpertsPerQuestionPerHourThreshold: expertsPerQuestionPerHourThreshold as BigDecimal,
            availabilityConfidenceScoreThreshold: confidenceScoreThreshold as BigDecimal,
        ]
    }
}

