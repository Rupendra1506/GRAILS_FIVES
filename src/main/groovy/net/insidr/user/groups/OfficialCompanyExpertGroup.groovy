package net.insidr.user.groups

import net.insidr.util.RedisKeys

class OfficialCompanyExpertGroup extends CompanyExpertGroup {
    OfficialCompanyExpertGroup(companyTagId) {
        super(companyTagId, RedisKeys.buildOfficialExpertsKey(companyTagId))
    }
}
