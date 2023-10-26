package net.insidr.user.groups

import net.insidr.util.RedisKeys

class TierUnofficialCompanyExpertGroup extends TierCompanyExpertGroup {
    TierUnofficialCompanyExpertGroup(tier, companyTagId) {
        super(tier, companyTagId, null)
    }

    @Override
    def experts() {
        def tierKey = RedisKeys.buildTierKey(tier, companyTagId)
        def officialExpertsKey = RedisKeys.buildOfficialExpertsKey(companyTagId)
        return redisService.sdiff(tierKey, officialExpertsKey) as Set
    }

    @Override
    def replace(expertIds) { } // since key is null, and this isn't really a data object

    @Override
    def add(expertId) { } // since key is null, and this isn't really a data object
}
