package net.insidr.user.groups

class CompanyExpertGroup extends ExpertGroup {
    def companyTagId

    CompanyExpertGroup(companyTagId, key) {
        super(key)
        this.companyTagId = companyTagId
    }
}
