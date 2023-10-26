package net.insidr.config

class CompanyConfig {

    String companyTag
    Double availabilityExpertsPerQuestionPerHourThreshold
    Double availabilityConfidenceScoreThreshold
    Integer maxQuestionsPerHourPerExpert
    boolean absoluteMetricsEnabled = false
    Integer ratingGraceCount
    Integer delayBetweenBatchesInSeconds


    Date dateCreated
    Date lastUpdated

    static constraints = {
        companyTag unique: true
        availabilityExpertsPerQuestionPerHourThreshold nullable: true
        availabilityConfidenceScoreThreshold nullable: true
        maxQuestionsPerHourPerExpert nullable: true
        ratingGraceCount nullable: true
        delayBetweenBatchesInSeconds nullable: true
    }

}
