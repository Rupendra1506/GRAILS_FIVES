package net.insidr.util

import net.insidr.company.Company
import net.insidr.tag.Tag
import net.insidr.user.Expert
import net.insidr.user.UserTagAssociation
import net.insidr.user.OnlineStatus

class RedisKeys {
    static def getAllCompaniesKey() {
        return "${Company.name}:companies"
    }

    static def getOnlineExpertsKey() {
        return "${Expert.name}:expertGroup:${OnlineStatus.online.name()}"
    }

    static def buildOfficialExpertsKey(tagId) {
        return "${UserTagAssociation.name}:tagId:${tagId}:official"
    }

    static def buildExpertGroupKeyBase(tagId) {
        return "${Expert.name}:tagId:${tagId}:expertGroup"
    }

    static def buildTierKey(tier, tagId, suffix = null) {
        def end = suffix ? ":${suffix}" : ""
        return "${buildExpertGroupKeyBase(tagId)}:${tier.name()}${end}"
    }

    static def buildTierOverrideAdditionsKey(tier, tagId) {
        return "${buildExpertGroupKeyBase(tagId)}:${tier.name()}:overrideAdditions"
    }

    static def buildTierOnlineStatusKey(tier, tagId, onlineStatus) {
        return buildTierKey(tier, tagId, onlineStatus.name())
    }

    static def buildExpertNegativeFeedbackKey(tagId) {
        return "${Expert.name}:tagId:${tagId}:expertGroup:negativeFeedback"
    }

    static def buildExpertTagId(expertId, tagId) {
        expertId + "_" + tagId
    }

    static def buildCompanyQuestionsKey(companyId) {
        return "${Company.name}:${companyId}:questions"
    }

    static def buildCompanyPercentOfExpertsPerQuestionKey(companyId) {
        return "${Company.name}:${companyId}:percentOfExpertsPerQuestion"
    }

    static def buildCompanyAliasCompanyIdKey(alias) {
        return "${Company.name}:alias:${alias}"
    }

    static def buildTagQuestionsKey(tagId) {
        return "${Tag.name}:${tagId}:questions"
    }

}
