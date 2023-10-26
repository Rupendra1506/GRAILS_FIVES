package net.insidr.user

import net.insidr.company.Company
import net.insidr.user.groups.TierOverridesExpertGroup
import net.insidr.util.RedisKeys
import net.insidr.util.RedisUtil
import org.apache.commons.logging.LogFactory

class ExpertReputationOverride {
    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    ExpertReputationOverride(params = [:]) {
        redisService = RedisUtil.redisService
        internalMap << params
    }

    def getExpertTagId() { buildExpertTagId( internalMap.expertId, internalMap.tagId) }
    def getExpertId() { internalMap.expertId as Long }
    def getTagId() { internalMap.tagId as Long}
    def getCompany() { Company.findByTagId(internalMap.tagId) }
    def getOverallReputationForCompany() { internalMap.overallReputationForCompany }
    def getExpertTier() { overallReputationForCompany as MetricTiers }
    def getOtherTiers() {
        def tier = expertTier
        tier.getOtherTiers()
    }

    def addExpertToTierOverrides() {
        log.info "Adding expert ${internalMap.expertId} to override tier ${expertTier}"
        new TierOverridesExpertGroup(expertTier, tagId).add(internalMap.expertId)
        removeExpertFromOtherTierOverrides()
    }

    def removeExpertFromOtherTierOverrides() {
        log.info "Removing expert ${internalMap.expertId} from all override tiers besides ${expertTier}"
        expertTier.getOtherTiers().each {
            new TierOverridesExpertGroup(it, tagId).delete(internalMap.expertId)
        }
    }

    def removeExpertFromAllTierOverrides() {
        log.info "Removing expert ${internalMap.expertId} from all override tiers"
        MetricTiers.values().each {
            new TierOverridesExpertGroup(it, tagId).delete(internalMap.expertId)
        }
    }

    static createUpdateOrDeleteFrom(json) {
        if (json.overrideValue != "") {
            log.info "Creating routing override ${json.overrideValue} for expert ${json.expertId} and tag ${json.tagId}"
            return new ExpertReputationOverride(
                    expertId: Long.toString(json.expertId),
                    tagId: Long.toString(json.tagId),
                    overallReputationForCompany: json.overrideValue,
            ).save()
        } else {
            return findByExpertIdAndTagId(json.expertId as String, json.tagId as String)?.deleteOverride()
        }
    }

    static def findByExpertIdAndTagId(expertId, tagId) {
        new ExpertReputationOverride().loadExpertReputationOverride(expertId, tagId)
    }

    def buildDomainExpertTagKey(id = expertTagId) {
        "${ExpertReputationOverride.name}:${id}"
    }

    def buildExpertTagId(expertId, tagId) {
         RedisKeys.buildExpertTagId(expertId, tagId)
    }

    def loadExpertReputationOverride(expertId, tagId) {
        def byExpertKey = buildDomainExpertTagKey(buildExpertTagId(expertId, tagId))

        internalMap << redisService.hgetAll(byExpertKey)
        if (internalMap.isEmpty()) {
            return null
        } else {
            return this
        }
    }
    
    def save() {
        redisService.hmset(buildDomainExpertTagKey(), internalMap)
        addExpertToTierOverrides()
        return this
    }

    def deleteOverride() {
        def expertTagId = buildDomainExpertTagKey(expertTagId)
        internalMap << redisService.hgetAll(expertTagId)
        internalMap.keySet().each {
            redisService.hdel(expertTagId, it)
            log.info "Deleting routing override ${it} from expert with id ${expertTagId}"
        }
        removeExpertFromAllTierOverrides()
    }

}
