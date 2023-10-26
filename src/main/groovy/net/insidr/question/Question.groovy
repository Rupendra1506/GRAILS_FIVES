package net.insidr.question

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Question {

    static final undefinedUserId = -1000L

    long questionId
    long userId
    String subject
    String text
    String permalink
    int responseCount
    Date dateCreated
    boolean useLanguageBasedRouting
    boolean languagesEnabledForCompany
    boolean languagesWithNativeSupportForCompany
    boolean languagesWithWaterfallSupportForCompany
    Long defaultRoutingLanguage
    boolean useCertificationBasedRouting
    Integer oneOnOneRoutingBatchSize
    int routingDelayInSeconds = 0
    int companyId

    List companies = []
    List languages = []
    List certifications = []

    static buildQuestionFrom(questionJson, posterJson) {
        def question = new Question(
                questionId: questionJson.id,
                userId: posterJson.id,
                subject: questionJson.subject,
                text: questionJson.text,
                permalink: questionJson.permalink,
                responseCount: questionJson.responseCount,
                dateCreated: Date.from(LocalDateTime.parse(questionJson.dateCreated, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)),
                useLanguageBasedRouting: questionJson.useLanguageBasedRouting,
                languagesWithNativeSupportForCompany: questionJson.languagesWithNativeSupportForCompany || false,
                languagesWithWaterfallSupportForCompany: questionJson.languagesWithWaterfallSupportForCompany || false,
                defaultRoutingLanguage: questionJson.defaultRoutingLanguage,
                useCertificationBasedRouting: questionJson.useCertificationBasedRouting,
                oneOnOneRoutingBatchSize: questionJson.oneOnOneRoutingBatchSize,
                routingDelayInSeconds: questionJson.routingDelayInSeconds ?: 0,
                companyId: questionJson.companies[0].id,
        )

        question.companies.addAll questionJson.tags.companies
        question.languages.addAll questionJson.tags.languages
        question.certifications.addAll questionJson.tags.certifications

        return question
    }

}
