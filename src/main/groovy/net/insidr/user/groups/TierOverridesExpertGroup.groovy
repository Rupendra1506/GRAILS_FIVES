package net.insidr.user.groups

import net.insidr.util.RedisKeys

class TierOverridesExpertGroup extends TierCompanyExpertGroup {
    TierOverridesExpertGroup(tier, companyTagId) {
        super(tier, companyTagId, RedisKeys.buildTierOverrideAdditionsKey(tier, companyTagId))
    }
}
