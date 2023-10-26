package net.insidr.company

import net.insidr.util.RedisUtil
import org.apache.commons.logging.LogFactory

class CompanyReputation {
    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    CompanyReputation(params = [:]) {
        redisService = RedisUtil.getRedisService()

        internalMap << params
    }

    def getTagId() { internalMap.tagId }

    def getCsat() { internalMap.csat }
    def getResolutionRate() { internalMap.resolutionRate }
    def getActionRate() { internalMap.actionRate }
    def getFeedbackRate() { internalMap.feedbackRate }
    def getAnswerQuality() { internalMap.answerQuality }

    static createFrom(json, tagId) {
        def feedback = new CompanyReputation(
                tagId: Long.toString(tagId),
                csat: json.csat as String,
                resolutionRate: json.resolutionRate as String,
                actionRate: json.actionRate as String,
                feedbackRate: json.feedbackRate as String,
                answerQuality: json.answerQuality as String,
        ).save()

        return feedback
    }

    def save() {
        redisService.hmset(buildByCompanyTagKey(tagId), internalMap)
        return this
    }

    static def findByTagId(tagId) {
        new CompanyReputation().loadCompanyReputationByTagId(tagId)
    }

    def loadCompanyReputationByTagId(tagId) {
        internalMap << redisService.hgetAll(buildByCompanyTagKey(tagId))
        if (internalMap.isEmpty()) {
            return null
        }

        return this
    }

    static def buildByCompanyTagKey(id){
        "${CompanyReputation.name}:tagId:${id}:metrics"
    }

}
