package net.insidr.company

import net.insidr.config.CompanyConfig

class ConfigService {

    def findOrCreateCompanyConfig(companyTag) {
        def companyConfig = CompanyConfig.findByCompanyTag(companyTag)
        if (!companyConfig) {
            companyConfig = new CompanyConfig(companyTag: companyTag)
        }

        return companyConfig
    }
}
