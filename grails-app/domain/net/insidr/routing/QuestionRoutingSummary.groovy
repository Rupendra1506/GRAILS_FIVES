package net.insidr.routing

import net.insidr.config.CompanyConfig

class QuestionRoutingSummary implements Serializable {

    long questionId
    String companyTag

    int totalRoutedExperts
    int totalRoutableExperts
    int numBatches

    String selectedTier

    Integer maxQuestionsPerHourPerExpert
    int questionsPerHour

    Double percentOfExpertsPerQuestion

    Date dateCreated

    static constraints = {
        maxQuestionsPerHourPerExpert nullable: true
    }

    static mapping = {
        version false
    }

    QuestionRoutingSummary(RoutingInstructions routingInstructions) {
        this.questionId = routingInstructions.question.questionId
        this.companyTag = routingInstructions.company.alias
        this.totalRoutedExperts = routingInstructions.totalRoutedExperts
        this.totalRoutableExperts = routingInstructions.totalRoutableExperts
        this.numBatches = routingInstructions.routingPlan*.delay?.unique()?.size() ?: 0
        this.selectedTier = routingInstructions.routingTier
        this.maxQuestionsPerHourPerExpert = CompanyConfig.findByCompanyTag(routingInstructions.company.alias)?.maxQuestionsPerHourPerExpert
        this.questionsPerHour = routingInstructions.questionsPerHour
        this.percentOfExpertsPerQuestion = routingInstructions.percentOfExpertsPerQuestion
    }
}
