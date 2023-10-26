package net.insidr.routing

import org.springframework.transaction.annotation.Transactional
import groovy.json.JsonSlurper
import net.insidr.company.Company
import net.insidr.company.CompanyQuestionsSchedule
import net.insidr.config.CompanyConfig
import net.insidr.user.ExpertType
import net.insidr.user.MetricTiers
import net.insidr.user.OnlineStatus
import net.insidr.user.UserTagAssociation
import net.insidr.user.groups.TierOnlineStatusExpertGroup

import net.insidr.util.RedisKeys

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Transactional
class RoutingClassificationService {

    def grailsApplication
    def redisService
    def companyService

    def random = new Random()

    def ESCALATED = "escalated"
    def WATERFALL_ROUTING = false

    def getRandomRoutingChanceAndTier() {
        def randomRoutingChance = random.nextDouble()

        def tier
        if (randomRoutingChance > grailsApplication.config.routing.tierPercentages.tier2) {
            tier = MetricTiers.TIER_1
        } else {
            tier = MetricTiers.TIER_2
        }

        return [ randomRoutingChance, tier ]
    }

    def determineRoutingInstructions(question, companyId) {
        def company = Company.findByCompanyId(companyId)

        def (routingChance, routingTier) = getRandomRoutingChanceAndTier()

        def routingPlan

        if(question.useLanguageBasedRouting && question.languagesWithWaterfallSupportForCompany) {
            WATERFALL_ROUTING = true
            routingPlan = getRoutingPlanForCompanyAndTierAndExpertType(question, company, routingTier)
        } else {
            WATERFALL_ROUTING = false
            routingPlan = getRoutingPlanForCompanyAndTier(question, company, routingTier)
        }
        def routingInstructions = new RoutingInstructions(
                question: question,
                company: company,
                routingTier: routingTier.toString(),
                targetPercentage: routingChance,
                routingPlan: routingPlan.plan,
                totalRoutedExperts: routingPlan.totalRoutedExperts,
                totalRoutableExperts: routingPlan.totalRoutableExperts,
                questionsPerHour: routingPlan.questionsPerHour,
                percentOfExpertsPerQuestion: routingPlan.percentOfExpertsPerQuestion,
        )

        return routingInstructions
    }

    def getRoutingPlanForCompanyAndTier(question, company, tier) {
        def batches = getBatchesForCompanyAndTier(company, tier, question.companyId)

        if (question.useLanguageBasedRouting && question.languages) {
            filterBatchesByQuestionLanguages(batches, question)
        }

        if (question.useCertificationBasedRouting && question.certifications) {
            filterBatchesByQuestionCertifications(batches, question)
        }

        pruneEmptyBatches(batches)

        return getRoutingPlanWithDelays(batches, company)
    }

    def getRoutingPlanForCompanyAndTierAndExpertType(question, company, tier) {
        def certificationRoutingEnabled = question.useCertificationBasedRouting && question.certifications

        def batches = getBatchesForCompanyAndTierAndExpertType(question, company, tier)

        if (certificationRoutingEnabled) {
            filterBatchesByQuestionCertifications(batches, question)
        }

        pruneEmptyBatches(batches)

        return getRoutingPlanWithDelays(batches, company)
    }

    private getBatchesForCompanyAndTier(company, tier, companyId) {
        def tagId = company.tagId

        def tier1RoutingChance = grailsApplication.config.routing.tierPercentages.tier1
        def tier2RoutingChance = grailsApplication.config.routing.tierPercentages.tier2

        def batches = []

        if (tier == MetricTiers.TIER_1) {
            batches = [
                createRoutingBatch(tagId, MetricTiers.TIER_1, OnlineStatus.online, tier1RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_2, OnlineStatus.online, tier2RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_1, OnlineStatus.offline, tier1RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_2, OnlineStatus.offline, tier2RoutingChance, companyId),
            ]
        } else if (tier == MetricTiers.TIER_2) {
            batches = [
                createRoutingBatch(tagId, MetricTiers.TIER_2, OnlineStatus.online, tier2RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_1, OnlineStatus.online, tier1RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_2, OnlineStatus.offline, tier2RoutingChance, companyId),
                createRoutingBatch(tagId, MetricTiers.TIER_1, OnlineStatus.offline, tier1RoutingChance, companyId),
            ]
        }
        return batches
    }

    private getBatchesForCompanyAndTierAndExpertType(question, company, tier) {
        def tagId = company.tagId

        def tier1RoutingChance = grailsApplication.config.routing.tierPercentages.tier1
        def tier2RoutingChance = grailsApplication.config.routing.tierPercentages.tier2

        def batches = []

        if (tier == MetricTiers.TIER_1) {
            batches = [
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.online, ExpertType.NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.online, ExpertType.NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.online, ExpertType.NON_NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.online, ExpertType.NON_NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.offline, ExpertType.NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.offline, ExpertType.NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.offline, ExpertType.NON_NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.offline, ExpertType.NON_NATIVE, tier2RoutingChance),
            ]
        } else if (tier == MetricTiers.TIER_2) {
            batches = [
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.online, ExpertType.NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.online, ExpertType.NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.online, ExpertType.NON_NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.online, ExpertType.NON_NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.offline, ExpertType.NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.offline, ExpertType.NATIVE, tier1RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_2, OnlineStatus.offline, ExpertType.NON_NATIVE, tier2RoutingChance),
                    createRoutingBatchByExpertType(question, tagId, MetricTiers.TIER_1, OnlineStatus.offline, ExpertType.NON_NATIVE, tier1RoutingChance),
            ]
        }
        log.debug("Expert ID's for Waterfall Routing : ${batches.expertIds}")
        return batches
    }

    private getRoutingPlanWithDelays(batches, company) {
        def companyConfig = CompanyConfig.findByCompanyTag(company.alias)
        def delayInSeconds = 0
        def additionalDelayInSeconds = (companyConfig?.delayBetweenBatchesInSeconds) ?: grailsApplication.config.routing.defaultDelayInSeconds

        // Checking for additionalDelayInSeconds value to prevent division by zero exception
        if(WATERFALL_ROUTING && additionalDelayInSeconds > 0) {
            additionalDelayInSeconds = additionalDelayInSeconds/2
        }

        def totalRoutedExperts = 0
        def isLessThanMaxQuestionToExpertRatio
        def questionsPerHour = getQuestionsPerHour(company)

        for (batch in batches) {
            log.debug("Total routed experts : ${totalRoutedExperts}, Questions per hour : ${questionsPerHour}")
            // Check totalRoutedExperts to prevent division by zero
            isLessThanMaxQuestionToExpertRatio = totalRoutedExperts && (questionsPerHour / totalRoutedExperts < grailsApplication.config.routing.maxQuestionToExpertRatio)

            // If there is a higher question-to-expert ratio than the configured, then keep the delay as is to route to more experts at once
            if (isLessThanMaxQuestionToExpertRatio) {
                delayInSeconds += additionalDelayInSeconds
            }
            log.debug("Delay between batches for Routing classification : ${batch.classification} is : ${delayInSeconds}")
            totalRoutedExperts += batch.expertIds.size()

            batch.delay = delayInSeconds
            batch.alertRestriction = company.alertRestriction
        }

        def routingPlan = filterBatchesByPercentOfExperts(batches, company, questionsPerHour)

        return routingPlan
    }

    def getQuestionsPerHour(company) {
        // In order to be more sensitive for surges, get question volume for a fraction of an hour, then multiply to reflect the full hour
        def lookbackPeriodInMinutes = grailsApplication.config.routing.questionLookbackPeriodInMinutes
        def questionVolumeOfLookbackPeriod = company.getQuestionCount(lookbackPeriodInMinutes)

        return questionVolumeOfLookbackPeriod * (60 / lookbackPeriodInMinutes)
    }

    private filterBatchesByQuestionLanguages(batches, question) {
        def languageExpertIds = []
        if (question.languagesWithNativeSupportForCompany) {
            languageExpertIds = question.languages.collect { redisService.smembers("${UserTagAssociation.name}:tagId:${it}:unofficial") }.flatten() as Set
            log.debug("Expert ID's for Native Speakers are : ${languageExpertIds}")
        } else if (question.defaultRoutingLanguage) {
            languageExpertIds = redisService.smembers("${UserTagAssociation.name}:tagId:${question.defaultRoutingLanguage}:unofficial")
            log.debug("Expert ID's for Machine Translation are : ${languageExpertIds}")
        }
        removeExpertIdsFromBatches(languageExpertIds, batches)
    }

    private filterExpertsByExpertType(expertIds, expertType, question) {
        def nativeExpertIds = question.languages.collect { redisService.smembers("${UserTagAssociation.name}:tagId:${it}:unofficial") }.flatten() as Set
        def expertIdsToFilter = []
        if(expertType == ExpertType.NATIVE) {
            expertIdsToFilter = nativeExpertIds
        } else if(expertType == ExpertType.NON_NATIVE) {
            for(expertId in expertIds) {
                if(!nativeExpertIds.contains(expertId)) {
                    expertIdsToFilter.add(expertId)
                }
            }
        }
        return getRoutableExpertIds(expertIdsToFilter, expertIds)
    }

    private filterBatchesByQuestionCertifications(batches, question) {
        def certificationExpertIds = question.certifications.collect { redisService.smembers("${UserTagAssociation.name}:tagId:${it}:unofficial") }.flatten() as Set
        removeExpertIdsFromBatches(certificationExpertIds, batches)
    }

    def filterBatchesByPercentOfExperts(batches, company, questionsPerHour) {
        def totalRoutableExperts = batches.inject(0) { total, batch -> total + batch.expertIds.size() }

        def percentOfExpertsPerQuestion = companyService.getPercentOfExpertsPerQuestion(company, totalRoutableExperts, questionsPerHour)

        for (batch in batches) {
            batch.expertIds = reduceExpertIdsToPercentage(batch.expertIds, percentOfExpertsPerQuestion)
        }

        def totalRoutedExperts = batches.inject(0) { total, batch -> total + batch.expertIds.size() }

        return [ plan: batches, totalRoutableExperts: totalRoutableExperts, totalRoutedExperts: totalRoutedExperts, questionsPerHour: questionsPerHour.intValue(), percentOfExpertsPerQuestion: percentOfExpertsPerQuestion ]
    }

    def reduceExpertIdsToPercentage(fullExpertIdList, percentToRetain) {
        def batchSize = fullExpertIdList.size()
        def numberOfExpertsToRetain = Math.ceil(batchSize * percentToRetain)
        def retainedExpertIdList = []

        def randomStartingPosition = random.nextInt(batchSize)

        for (int i = 0; i < numberOfExpertsToRetain; i++) {
            def index = (i + randomStartingPosition) % batchSize
            retainedExpertIdList.add(fullExpertIdList[index])
        }

        return retainedExpertIdList
    }

    static private removeExpertIdsFromBatches(expertIds, batches) {
        for (batch in batches) {
            batch.expertIds.removeIf { !expertIds.contains(it) }
        }
    }

    private getRoutableExpertIds(expertIdsToFilter, expertIds) {
        def routableExpertIds = []
        for (expertId in expertIds) {
            if(expertIdsToFilter.contains(expertId)) {
                routableExpertIds.add(expertId)
            }
        }
        return routableExpertIds
    }

    static private pruneEmptyBatches(batches) {
        batches.removeIf { batch -> batch.expertIds.size() == 0 }
    }

    def getEscalationRoutingInstructions(question, companyId) {
        def company = Company.findByCompanyId(companyId)
        def officialExperts = UserTagAssociation.findByTagIdAndOfficial(company.tagId)
        if (!officialExperts) {
            return null
        }

        def officialExpertsClassification = "${RedisKeys.buildExpertGroupKeyBase(company.tagId)}:official"
        def officialExpertsRoutingPlan = [ new RoutingBatch(officialExperts, officialExpertsClassification, 1.0) ]

        def routingInstructions = new RoutingInstructions(
                question: question,
                company: company,
                routingTier: ESCALATED,
                targetPercentage: 1.0,
                routingPlan: officialExpertsRoutingPlan,
                totalRoutedExperts: officialExperts.size(),
                questionsPerHour: getQuestionsPerHour(company),
                percentOfExpertsPerQuestion: 1.0
        )

        return routingInstructions
    }

    private createRoutingBatch(companyTagId, tier, onlineStatus, routingChance, companyId) {
        def routingClassification = buildRoutingClassification(companyTagId, tier, onlineStatus)
        def expertIds = new TierOnlineStatusExpertGroup(onlineStatus, tier, companyTagId).experts()

        def filteredExpertIds = filterExpertsByActiveOpenQuestionLimit(expertIds,companyId)
        //pass filtered experts in routing batch
        return new RoutingBatch(filteredExpertIds, routingClassification, routingChance)
    }

    private createRoutingBatchByExpertType(question, companyTagId, tier, onlineStatus, expertType, routingChance) {
        def routingClassification = buildRoutingClassificationByExpertType(companyTagId, tier, expertType, onlineStatus)
        def expertIds = new TierOnlineStatusExpertGroup(onlineStatus, tier, companyTagId).experts()
        def filteredExpertIds = filterExpertsByActiveOpenQuestionLimit(expertIds,question.companyId)
        //pass filtered experts to routableExpertIds
        def routableExpertIds = filteredExpertIds
        if (question.languages) {
            routableExpertIds = filterExpertsByExpertType(expertIds, expertType, question)
        }
        log.debug("Expert ID's for ${tier}-${expertType}-${onlineStatus} are ${routableExpertIds}")
        return new RoutingBatch(routableExpertIds, routingClassification, routingChance)
    }

    private buildRoutingClassification(companyTagId, tier, onlineStatus) {
        return "${companyTagId}:${tier.name()}:${onlineStatus}"
    }

    private buildRoutingClassificationByExpertType(companyTagId, tier, onlineStatus, expertType) {
        return "${companyTagId}:${tier.name()}:${expertType}:${onlineStatus}"
    }

    private filterExpertsByActiveOpenQuestionLimit(expertIds,companyId) {
        def companyScheduleInfo = redisService.hgetAll("${CompanyQuestionsSchedule.name}:${companyId}")
        def filteredExpertIds = []
        if (companyScheduleInfo!=null && companyScheduleInfo.enableActiveOpenQuestions && companyScheduleInfo.enableActiveOpenQuestions.toString().equalsIgnoreCase("yes")) {
            if (checkIfCurrentDateIsWithinSchedule(companyScheduleInfo.scheduleStartDate.toString(), companyScheduleInfo.scheduleEndDate.toString())) {
                int numberOfQuestions = getNumberOfAllowedQuestionsPerExpert(companyScheduleInfo.companyScheduleSlotsDetails)
                def jsonSlurperExperts = new JsonSlurper()
                if(companyScheduleInfo.expertsOpenQuestionsCount){
                def parsedValueExperts = jsonSlurperExperts.parseText(companyScheduleInfo.expertsOpenQuestionsCount)

                parsedValueExperts.each{expertOpenQuestion ->
                    expertOpenQuestion.each{expertId,openQuestionCount ->
                    if(expertIds.contains(expertId)){
                        if(!(numberOfQuestions!=0 && openQuestionCount>=numberOfQuestions)){
                            filteredExpertIds.add(expertId)
                        }
                    }
                    }
                }
                }
                else{
                    return expertIds
                }

            }
            else{
                return expertIds
            }
        }
        else{
            return expertIds
        }
        return filteredExpertIds
    }

    int getNumberOfAllowedQuestionsPerExpert(def companyScheduleSlotsDetails) {
        def jsonSlurper = new JsonSlurper()
        def parsedValue = jsonSlurper.parseText(companyScheduleSlotsDetails)

        def timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        ZonedDateTime currentTime = ZonedDateTime.now()
        def utcTime = currentTime.withZoneSameInstant(ZoneOffset.UTC).format(timeFormatter)
        int numberOfQuestions = 0
        parsedValue.eachWithIndex { item, index ->
            String startTimeString = item["startTimeslot${index+1}"]
            String endTimeString = item["endTimeslot${index+1}"]
            Long questionLimit = item["numberOfQuestions${index+1}"].toLong()

            def startTime = LocalTime.parse(startTimeString, timeFormatter)
            def endTime = LocalTime.parse(endTimeString, timeFormatter)
            def formattedTime = LocalTime.parse(utcTime, timeFormatter)

            log.info "CurrentTime: ${currentTime}"
            log.info "CurrentTime To UTC: ${utcTime}"
            log.info "CurrentTime To UTC(Formatted Time): ${formattedTime}"
            log.info "Time_Range From Redis------ startTimeslot${index+1} is ${endTimeString}"
            log.info "Time_Range From Redis After Parsing------ startTimeslot${index+1} is ${startTime}"

            log.info "Time_Range From Redis------ endTimeslot${index+1} is ${startTimeString}"
            log.info "Time_Range From Redis After Parsing------ endTimeslot${index+1} is ${endTime}"


            if(formattedTime.isAfter(startTime) && formattedTime.isBefore(endTime)){
                numberOfQuestions =  questionLimit.intValue()
            }
        }
        return numberOfQuestions
    }

    boolean checkIfCurrentDateIsWithinSchedule(String startDateStr, String endDateStr){
        def formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
       def startDate = LocalDateTime.parse(startDateStr, formatter)
        def endDate = LocalDateTime.parse(endDateStr, formatter)
        def currentDate = LocalDateTime.now()

        return currentDate.isAfter(startDate) && currentDate.isBefore(endDate)
    }
}
