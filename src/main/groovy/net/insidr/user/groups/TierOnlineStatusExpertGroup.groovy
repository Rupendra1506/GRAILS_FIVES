package net.insidr.user.groups

import net.insidr.util.RedisKeys

class TierOnlineStatusExpertGroup extends TierCompanyExpertGroup {
    TierOnlineStatusExpertGroup(onlineStatus, tier, companyTagId) {
        super(tier, companyTagId, RedisKeys.buildTierOnlineStatusKey(tier, companyTagId, onlineStatus))
    }
}
