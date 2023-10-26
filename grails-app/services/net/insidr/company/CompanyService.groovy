package net.insidr.company

import net.insidr.config.CompanyConfig
import net.insidr.util.RedisKeys

class CompanyService {

    def grailsApplication
    def redisService

    def HUGE_QUESTIONS_PER_ONLINE_EXPERTS_PER_HOUR_THRESHOLD = 1000
    def MAX_CONFIDENCE_SCORE = 0.90
    static final FIVE_MINUTES_IN_SECONDS = 5 * 60

    def pruneQuestions() {
        def companyIds = redisService.smembers("${Company.name}:companies")
        companyIds.each { Company.findByCompanyId(it)?.pruneQuestionEntries() }
    }

    def getExpertAvailabilityMetrics(companyAlias, certificationTagIds=[]) {
        def company = Company.findByAlias(companyAlias)
        def companyConfig = company.getAvailabilityConfig()
        def confidenceScoreThreshold = companyConfig.availabilityConfidenceScoreThreshold
        def expertsPerQuestionPerHourThreshold = companyConfig.availabilityExpertsPerQuestionPerHourThreshold

        // If expertsPerHourThreshold is 0 or doesn't exist, assume that the inverse, questionsPerExpertPerHourThreshold, is extremely high number
        def isThresholdUsable = expertsPerQuestionPerHourThreshold != null && expertsPerQuestionPerHourThreshold != 0
        def questionsPerOnlineExpertsPerHourThreshold = isThresholdUsable ? 1 / expertsPerQuestionPerHourThreshold : HUGE_QUESTIONS_PER_ONLINE_EXPERTS_PER_HOUR_THRESHOLD

        // Important note:  questions are filtered by certifications, but not by language, so getExpertAvailablityMetrics doesn't work for cases where a specific language is required
        def questionsPerHour = company.getQuestionsWithCertificationsCount(certificationTagIds, 15) * 4 // 15 min lookback * 4 = 1 hour
        def onlineExpertCount = company.getOnlineExpertsWithCertificationsCount(certificationTagIds)
        def onlineExpertsPerQuestionPerHour = questionsPerHour ? onlineExpertCount / questionsPerHour : onlineExpertCount
        if (onlineExpertsPerQuestionPerHour == 0) {
            return [
                confidenceScore: 0,
                threshold:   confidenceScoreThreshold,
                onlineExpertCount: onlineExpertCount,
                questionsPerHour: questionsPerHour,
                questionsPerOnlineExpertPerHourThreshold: questionsPerOnlineExpertsPerHourThreshold,
            ]
        }

        def bConstant = expertsPerQuestionPerHourThreshold - expertsPerQuestionPerHourThreshold * confidenceScoreThreshold
        def confidenceScore = 1 - (bConstant / onlineExpertsPerQuestionPerHour)

        // Don't return higher than .9, which is 90% confidence that the question will be quickly claimed by an expert
        return [
            confidenceScore: Math.min(MAX_CONFIDENCE_SCORE, confidenceScore),
            threshold: confidenceScoreThreshold,
            onlineExpertCount: onlineExpertCount,
            questionsPerHour: questionsPerHour,
            questionsPerOnlineExpertPerHourThreshold: questionsPerOnlineExpertsPerHourThreshold,
        ]
    }

    def getPercentOfExpertsPerQuestion(company, routableExpertCount, questionsPerHour) {
        if (!routableExpertCount) {
            return 0
        }

        def percentOfExpertsPerQuestionKey = RedisKeys.buildCompanyPercentOfExpertsPerQuestionKey(company.companyId)
        def percentOfExpertsPerQuestion = redisService.get(percentOfExpertsPerQuestionKey) as BigDecimal
        if (percentOfExpertsPerQuestion) {
            return percentOfExpertsPerQuestion
        }

        def companyConfig = CompanyConfig.findByCompanyTag(company.alias)
        def maxQuestionsPerHourPerExpert = companyConfig?.maxQuestionsPerHourPerExpert
        if (!maxQuestionsPerHourPerExpert) {
            return 1.0
        }

        def currentQuestionsPerHourPerExpert = questionsPerHour / routableExpertCount
        percentOfExpertsPerQuestion = currentQuestionsPerHourPerExpert ? maxQuestionsPerHourPerExpert / currentQuestionsPerHourPerExpert : 1

        if (percentOfExpertsPerQuestion > 1) {
            percentOfExpertsPerQuestion = 1
        }

        redisService.set(percentOfExpertsPerQuestionKey, percentOfExpertsPerQuestion as String)
        redisService.expire(percentOfExpertsPerQuestionKey, FIVE_MINUTES_IN_SECONDS)

        return percentOfExpertsPerQuestion
    }

}
