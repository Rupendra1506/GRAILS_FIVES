package net.insidr.user

import grails.util.Holders
import net.insidr.routing.RoutingClassification
import org.apache.commons.logging.LogFactory

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Expert {

    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    Expert(params = [:]) {
        try {
            redisService = Holders.grailsApplication.mainContext.getBean "redisService"
        } catch (any) {
            log.error "could not get redisService", any
        }

        internalMap << params
    }

    def getUserId() { internalMap.userId as long}

    def getRoutingClassification() { internalMap.routingClassification as RoutingClassification }
    def setRoutingClassification(routingClassification) { internalMap.routingClassification = routingClassification as String }

    def getQuestionSetting() { internalMap.questionSetting }
    def setQuestionSetting(questionSetting) { internalMap.questionSetting = questionSetting }

    def getRewardSetting() { internalMap.rewardSetting }
    def setRewardSetting(rewardSetting) { internalMap.rewardSetting = rewardSetting }

    def getLastActiveDate() { new Date(internalMap.lastActiveDate as long) }
    def setLastActiveDate(date) { internalMap.lastActiveDate = date.time as String }

    def getHasMobileDevices() { Boolean.valueOf(internalMap.hasMobileDevices) }
    def setHasMobileDevices(hasMobileDevices) { internalMap.hasMobileDevices = hasMobileDevices as String }

    def getLastAlert() { (internalMap.lastAlert) ? new Date(internalMap.lastAlert as long) : null }
    def setLastAlert(date) { internalMap.lastAlert = date.getTime() as String }

    def getActivityLevel() { (internalMap.activityLevel) ? internalMap.activityLevel as double : null }
    def setActivityLevel(activityLevel) { internalMap.activityLevel = activityLevel as String }

    def setName(name) { internalMap.name = name }
    def setEmailAddress(emailAddress) { internalMap.emailAddress = emailAddress }

    static createOrUpdateFrom(json) {
        def expert = findByUserId(json.id)
        if (expert) {
            log.info "Update existing ${expert} for ${json.id} ..."

            expert.name = json.name
            expert.emailAddress = json.emailAddress
            expert.routingClassification = json.routingClassification
            expert.questionSetting = json.notificationSettings.question
            expert.rewardSetting = json.notificationSettings.reward
            expert.hasMobileDevices = json.hasMobileDevices
            expert.lastActiveDate = Date.from(LocalDateTime.parse(json.lastActiveDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC))
            expert.save()
        } else {
            log.info "Creating new expert for ${json.id} ..."

            expert = new Expert(
                    userId: json.id as String,
                    name: json.name,
                    emailAddress: json.emailAddress,
                    routingClassification: json.routingClassification,
                    questionSetting: json.notificationSettings.question,
                    rewardSetting: json.notificationSettings.reward,
                    hasMobileDevices: json.hasMobileDevices as String,
                    lastActiveDate: Date.from(LocalDateTime.parse(json.lastActiveDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)).getTime() as String,
            ).save()
        }
        def addedTags
        def deletedTags
        (addedTags, deletedTags) = UserTagAssociation.updateFrom(expert.userId, json.tags)

        ExpertReputation.updateExpertTiers(expert.userId, addedTags, deletedTags)

        return expert
    }

    static def updateAlertRestriction(json) {
        def expert = Expert.findByUserId(json.id)
        if (expert) {
            expert.lastAlert = Date.from(LocalDateTime.parse(json.lastAlertTime, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC))
            expert.activityLevel = json.activityLevel
            expert.save()
        }
        return expert
    }

    def save() {
        redisService.hmset("${Expert.name}:${internalMap.userId}", internalMap)
        return this
    }

    static def findByUserId(userId) {
        def expert = new Expert()
        expert.loadInternalMapByUserId(userId)
    }

    def loadInternalMapByUserId(userId) {
        internalMap << redisService.hgetAll("${Expert.name}:${userId}")
        if (internalMap.isEmpty()) {
            return null
        }

        return this
    }

}
