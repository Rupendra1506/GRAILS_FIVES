package net.insidr.user.groups

import net.insidr.util.RedisKeys

class TierExpertGroup extends TierCompanyExpertGroup {
    TierExpertGroup(tier, companyTagId) {
        super(tier, companyTagId, RedisKeys.buildTierKey(tier, companyTagId))
    }
}
