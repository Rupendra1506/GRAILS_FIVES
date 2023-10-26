package net.insidr.user.groups

class TierCompanyExpertGroup extends CompanyExpertGroup {
    def companyTagId
    def tier

    TierCompanyExpertGroup(tier, companyTagId, key) {
        super(companyTagId, key)
        this.companyTagId = companyTagId
        this.tier = tier
    }
}
