package net.insidr.user

import net.insidr.company.Company
import net.insidr.user.groups.NegativeFeedbackExpertGroup
import net.insidr.user.groups.OnlineExpertGroup
import net.insidr.user.groups.TierOnlineStatusExpertGroup
import net.insidr.user.groups.TierOverridesExpertGroup
import net.insidr.user.groups.TierUnofficialCompanyExpertGroup
import net.insidr.util.RedisKeys

class ExpertGroupService {

    def redisService

    def pruneExpertNegativeFeedback() {
        def companyIds = redisService.smembers(RedisKeys.allCompaniesKey)
        companyIds.each {
            def tagId = Company.findByCompanyId(it)?.tagId
            new NegativeFeedbackExpertGroup(tagId as String).prune()
        }
    }

    def buildBaseExpertGroupsForAllCompanies() {
        def companyIds = redisService.smembers(RedisKeys.allCompaniesKey)
        companyIds.each {
            def tagId = Company.findByCompanyId(it)?.tagId
            buildBaseExpertGroupsForCompany(tagId as String)
        }
    }

    def buildBaseExpertGroupsForCompany(tagId) {
        buildBaseExpertGroupsForCompanyTier(tagId, MetricTiers.TIER_1)
        buildBaseExpertGroupsForCompanyTier(tagId, MetricTiers.TIER_2)
    }

    def buildBaseExpertGroupsForCompanyTier(tagId, tier) {
        def (onlineExperts, offlineExperts) = buildTierBaseGroups(tagId, tier)
        new TierOnlineStatusExpertGroup(OnlineStatus.online, tier, tagId).replace(onlineExperts)
        new TierOnlineStatusExpertGroup(OnlineStatus.offline, tier, tagId).replace(offlineExperts)
    }

    def buildTierBaseGroups(tagId, tier) {
        def experts = new TierUnofficialCompanyExpertGroup(tier, tagId).experts()
        experts = processOverrides(tagId, experts, tier)
        return createOnlineOfflineGroups(experts)
    }

    def processOverrides(tagId, initialTierExperts, tier) {
        def experts = removeOtherTierOverrides(tagId, initialTierExperts, tier)
        def overrideAdditions = new TierOverridesExpertGroup(tier, tagId).experts()
        experts = experts.plus(overrideAdditions)
        return experts as Set
    }

    def removeOtherTierOverrides(tagId, initialTierExperts, tier) {
        def experts = initialTierExperts
        for (otherTier in tier.otherTiers) {
            def otherTierOverrideAdditions = new TierOverridesExpertGroup(otherTier, tagId).experts()
            experts.removeIf { otherTierOverrideAdditions.contains(it) }
        }
        return experts as Set
    }

    def createOnlineOfflineGroups(unifiedGroup) {
        def onlineGroup = []
        def offlineGroup = []
        def onlineExperts = new OnlineExpertGroup().experts()
        unifiedGroup.each {
            if (onlineExperts.contains(it)) {
                onlineGroup << it
            } else {
                offlineGroup << it
            }
        }
        return [ onlineGroup, offlineGroup ]
    }

}
