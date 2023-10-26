package net.insidr.company

import grails.plugin.json.builder.JsonOutput
import grails.util.Holders
import org.apache.commons.logging.LogFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CompanyQuestionsSchedule {
    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    CompanyQuestionsSchedule(params = [:]) {
        try {
            redisService = Holders.grailsApplication.mainContext.getBean "redisService"
        } catch (any) {
            log.error "could not get redisService", any
        }

        internalMap << params
    }
    def getCompanyId() { internalMap.companyId as long }

    def getCompanyName() { internalMap.companyName }
    def setCompanyName(name) { internalMap.companyName = name }

    def getEnableActiveOpenQuestions() { internalMap.enableActiveOpenQuestions}
    def setEnableActiveOpenQuestions(enableActiveOpenQuestions) { internalMap.enableActiveOpenQuestions = enableActiveOpenQuestions }



    def getScheduleStartDate() { new Date(internalMap.scheduleStartDate as long) }
    def setScheduleStartDate(date) { internalMap.scheduleStartDate = date as String }

    def getScheduleEndDate() { new Date(internalMap.scheduleEndDate as long) }
    def setScheduleEndDate(date) { internalMap.scheduleEndDate = date as String }

    def getCompanyScheduleSlotsDetails() { internalMap.companyScheduleSlotsDetails }
    def setCompanyScheduleSlotsDetails(companyScheduleSlotsDetails) { internalMap.companyScheduleSlotsDetails = companyScheduleSlotsDetails }

    def getExpertsOpenQuestionsCount(){internalMap.expertsOpenQuestionsCount}
    def setExpertsOpenQuestionsCount(expertsOpenQuestionsCount){internalMap.expertsOpenQuestionsCount = expertsOpenQuestionsCount}



    static createOrUpdateFrom(json) {
        def companyQuestionsSchedule = findByCompanyId(json.companyId)
        def companyScheduleDetailsList= getCompanyScheduleDetailsList(json.companyScheduleDetails)
        if(companyQuestionsSchedule){
            log.info "Update existing CompanyQuestionsSchedule Record for companyId: ${json.companyId} ..."
            companyQuestionsSchedule.enableActiveOpenQuestions = json.enableActiveOpenQuestions
            companyQuestionsSchedule.scheduleStartDate = Date.from(LocalDateTime.parse(json.scheduleStartDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC))
            companyQuestionsSchedule.scheduleEndDate = Date.from(LocalDateTime.parse(json.scheduleEndDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC))
            companyQuestionsSchedule.companyScheduleSlotsDetails = JsonOutput.toJson(companyScheduleDetailsList)
            companyQuestionsSchedule.save()
        }
        else{
            log.info "Adding New CompanyQuestionsSchedule Record for companyId: ${json.companyId} ..."
            companyQuestionsSchedule = new CompanyQuestionsSchedule(
                    companyId: json.companyId as String,
                    companyName: json.companyName,
                    enableActiveOpenQuestions: json.enableActiveOpenQuestions,
                    scheduleStartDate: Date.from(LocalDateTime.parse(json.scheduleStartDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)) as String,
                    scheduleEndDate: Date.from(LocalDateTime.parse(json.scheduleEndDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)) as String,
                    companyScheduleSlotsDetails:{
                        return JsonOutput.toJson(companyScheduleDetailsList)
                    }()

            ).save()

        }
        return companyQuestionsSchedule
    }

    static def getCompanyScheduleDetailsList(def companyScheduleDetails) {
        def details = []
        for (int i = 0; i < companyScheduleDetails.size(); i++) {
            def record = companyScheduleDetails[i]
            log.info "Time_Range Before Parsing------ startTimeslot${i+1} is ${record.startTimeslot}"
            log.info "Time_Range Before Parsing------ endTimeslot${i+1} is ${record.endTimeslot}"
            String startTimeSlot = Date.from(LocalDateTime.parse(record.startTimeslot, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)) as String
            String endTimeSlot = Date.from(LocalDateTime.parse(record.endTimeslot, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)) as String

            log.info "Time_Range After Parsing------ startTimeslot${i+1} is ${startTimeSlot}"
            log.info "Time_Range After Parsing------ endTimeslot${i+1} is ${endTimeSlot}"

            log.info "Time_Range After Parsing and extract time------ startTimeslot${i+1} is ${extractTime(startTimeSlot)}"
            log.info "Time_Range After Parsing and extract time------ endTimeslot${i+1} is ${extractTime(endTimeSlot)}"
            def detailMap = [
                    "startTimeslot${i+1}" : extractTime(startTimeSlot),
                    "endTimeslot${i+1}" :extractTime(endTimeSlot),
                    "numberOfQuestions${i+1}" : record.numberOfQuestions as String
            ]
            details.add(detailMap)
        }
        return details
    }

    static def updateExpertOpenQuestions(json){
       def companyExpertQuestionCount = json
        companyExpertQuestionCount?.each{companyId,expertsOpenQuestionsCount ->
            def companyQuestionsSchedule = CompanyQuestionsSchedule.findByCompanyId(companyId)
            if(companyQuestionsSchedule && expertsOpenQuestionsCount){
                companyQuestionsSchedule.expertsOpenQuestionsCount = JsonOutput.toJson(expertsOpenQuestionsCount)
                companyQuestionsSchedule.save()
            }

        }

    }

def save() {
    redisService.hmset("${CompanyQuestionsSchedule.name}:${internalMap.companyId}", internalMap)
    return this
}
    static String extractTime(String dateString) {
        String[] parts = dateString.split(" ")
        String timeString = parts[3]
        return timeString
    }

    static def findByCompanyId(companyId) {
        def companyQuestionSchedule = new CompanyQuestionsSchedule()
        companyQuestionSchedule.loadInternalMapByUserId(companyId)
    }

    def loadInternalMapByUserId(companyId) {
        internalMap << redisService.hgetAll("${CompanyQuestionsSchedule.name}:${companyId}")
        if (internalMap.isEmpty()) {
            return null
        }
        return this
    }



}
