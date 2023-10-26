package net.insidr.user

import grails.util.Holders
import net.insidr.company.Company
import net.insidr.user.groups.TierExpertGroup
import net.insidr.user.groups.TierOnlineStatusExpertGroup
import net.insidr.user.groups.TierOverridesExpertGroup
import org.apache.commons.logging.LogFactory

class ExpertReputation {
    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    ExpertReputation(params = [:]) {
        try {
            redisService = Holders.grailsApplication.mainContext.getBean "redisService"
        } catch (any) {
            log.error "could not get redisService", any
        }

        internalMap << params
    }

    def getId() { internalMap.expertId + "_" + internalMap.tagId }
    def getExpertId() { internalMap.expertId as Long }
    def getTagId() { internalMap.tagId as Long}
    def getCompany() { Company.findByTagId(internalMap.tagId) }

    def getQuestionsAnswered() { internalMap.questionsAnswered as Long }

    def getOverallReputationForCompany() { ExpertReputationOverride.findByExpertIdAndTagId(expertId, tagId)?.overallReputationForCompany ?: internalMap.overallReputationForCompany }

    def getCsatBucket() { internalMap.csatBucket }
    def getCsatValue() { internalMap.csatValue }

    def getResolutionRateBucket() { internalMap.resolutionRateBucket }
    def getResolutionRateValue() { internalMap.resolutionRateValue }

    def getActionRateBucket() { internalMap.actionRateBucket }
    def getActionRateValue() { internalMap.actionRateValue }

    def getFeedbackRateBucket() { internalMap.feedbackRateBucket }
    def getFeedbackRateValue() { internalMap.feedbackRateValue }

    def getAnswerQualityBucket() { internalMap.answerQualityBucket }
    def getAnswerQualityValue() { internalMap.answerQualityValue }


    def getExpertTier() { overallReputationForCompany as MetricTiers }
    def getOtherTiers() {
        def tier = expertTier
        tier.getOtherTiers()
    }

    def addExpertToTier() {
        def shouldSkipAddingExpertToTier = !(tagId in UserTagAssociation.findByExpertId(expertId)*.tagId)

        if (shouldSkipAddingExpertToTier) {
            return
        }

        addToTierExclusively(internalMap.overallReputationForCompany as MetricTiers, tagId, internalMap.expertId)
    }

    def addToTierExclusively(MetricTiers tier, tagId, expertId) {
        new TierExpertGroup(tier , tagId).add(expertId)
        tier.getOtherTiers().each {
            new TierExpertGroup(it, tagId).delete(expertId)
        }
    }

    def addExpertToTier2() {
        addToTierExclusively(MetricTiers.TIER_2, tagId, internalMap.expertId)
    }

    def addExpertToTier2(expertId, tagId) {
        addToTierExclusively(MetricTiers.TIER_2, tagId, expertId)
    }

    def deleteExpertFromAllTiers(expertId, tagId) {
        MetricTiers.getAllTiers().each {
            new TierExpertGroup(it, tagId).delete(expertId)
            new TierOverridesExpertGroup(it, tagId).delete(expertId)
            new TierOnlineStatusExpertGroup(OnlineStatus.online, it, tagId).delete(expertId)
            new TierOnlineStatusExpertGroup(OnlineStatus.offline, it, tagId).delete(expertId)
        }
    }

    static updateExpertTiers(expertId, addedTags, deletedTags) {
        addedTags.each {
            def er = new ExpertReputation().findByExpertAndTagId(expertId, it)
            if (er) {
                er.addExpertToTier2()
            } else {
                new ExpertReputation().addExpertToTier2(expertId, it)
            }
        }

        deletedTags.each {
            new ExpertReputation().deleteExpertFromAllTiers(expertId, it)
        }
    }

    static createFrom(json, tagId) {
        def feedback = new ExpertReputation(
                expertId: Long.toString(json.expertId),
                tagId: Long.toString(tagId),
                questionsAnswered: json.questionsAnswered as String,
                overallReputationForCompany: json.overallReputationForCompany,
                csatBucket: json.csatBucket,
                csatValue: json.csatValue as String,
                resolutionRateBucket: json.resolutionRateBucket,
                resolutionRateValue: json.resolutionRateValue as String,
                actionRateBucket: json.actionRateBucket,
                actionRateValue: json.actionRateValue as String,
                feedbackRateBucket: json.feedbackRateBucket,
                feedbackRateValue: json.feedbackRateValue as String,
                answerQualityBucket: json.answerQualityBucket,
                answerQualityValue: json.answerQualityValue as String,
        ).save()

        return feedback
    }

    static def findAllByExpertId(expertId) {
        new ExpertReputation().loadExpertReputationsByExpertId(expertId)
    }

    static deleteAllByExpertId(expertId) {
        def reputations = ExpertReputation.findAllByExpertId(expertId)
        reputations.each {
            it.deleteExpertFromAllTiers(expertId, it.tagId)
            it.delete()
        }
        new ExpertReputation().deleteReputationsSet(expertId)
    }

    def findByExpertAndTagId(expertId, tagId) {
        buildByExpertTagKey(expertId + "_" + tagId)
        new ExpertReputation().loadMapByUserTagId(expertId)
    }

    def buildByExpertTagKey(expertTagId = id){
        "${ExpertReputation.name}:${expertTagId}"
    }

    def buildByExpertKey(expertId = internalMap.expertId) {
        "${ExpertReputation.name}:expertId:${expertId}:reputations"
    }

    def loadExpertReputationsByExpertId(expertId) {
        def byExpertKey = buildByExpertKey(expertId)
        def userTagIds = redisService.smembers(byExpertKey)
        return userTagIds.collect {
            new ExpertReputation().loadMapByUserTagId(it)
        }
    }

    def loadMapByUserTagId(expertTagId) {
        def byExpertTagKey = buildByExpertTagKey(expertTagId)
        internalMap << redisService.hgetAll(byExpertTagKey)
        if (internalMap.isEmpty()) {
            return null
        } else {
            return this
        }
    }

    def save() {
        redisService.hmset(buildByExpertTagKey(), internalMap)
        redisService.sadd(buildByExpertKey(), id)
        addExpertToTier()
        return this
    }

    def deleteReputationsSet(expertId) {
        redisService.del(buildByExpertKey(expertId))
    }

    def delete() {
        redisService.del(buildByExpertTagKey(expertId + "_" + tagId))
    }

}
