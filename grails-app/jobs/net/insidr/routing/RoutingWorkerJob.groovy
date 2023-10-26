package net.insidr.routing

import net.insidr.user.Expert
import net.insidr.util.SQLUtil
import org.joda.time.DateTime

import java.text.MessageFormat

class RoutingWorkerJob {

    def routingService

    static final secondInMillis = 1000L
    static final minuteInSeconds = 60
    static final hourInSeconds = 60 * minuteInSeconds
    static final hourInMillis = hourInSeconds * secondInMillis

    static triggers = {
        simple name: "RoutingWorker", repeatInterval: 1 * secondInMillis
    }

    def concurrent = false
    def description = "Process RoutingUnitOfWork to put questions on experts' notification feed"

    def host = System.getProperty("insidr.host") ?: InetAddress.localHost.hostName

    def dataSource
    def messageBusService

    def execute() {
        def start = System.currentTimeMillis()
        log.info "Start"

        def routingUnitsOfWorkInProgress = []
        RoutingUnitOfWork.withTransaction {
            try {
                if (SQLUtil.acquireRoutingLock(dataSource)) {
                    def routingUnitsOfWork = findRoutingUnitsOfWork()
                    // findAll() used instead of each() since findAll() will not append to list if false is returned
                    routingUnitsOfWorkInProgress = routingUnitsOfWork.findAll { routingService.markRoutingUnitOfWork(it, RoutingUnitOfWorkStatus.IN_PROGRESS) }
                } else {
                    log.info "Could not get routing lock"
                }
            } catch (any) {
                log.error "Unable to mark RoutingUnitOfWork as ${RoutingUnitOfWorkStatus.IN_PROGRESS}", any
            } finally {
                SQLUtil.releaseRoutingLock(dataSource)
            }
        }

        routingUnitsOfWorkInProgress.each { processRoutingUnitOfWork it }

        def stop = System.currentTimeMillis()
        log.info "Stop: ${(stop - start)} msec."
    }

    def findRoutingUnitsOfWork(now = new DateTime()) {
        def unitsOfWork = findStaleRoutingUnitsOfWork(now)

        def maxBatchSize = grailsApplication?.config?.routing?.worker?.batchSize ?: 500
        if (unitsOfWork.size() < maxBatchSize) {
            unitsOfWork += findUnclaimedRoutingUnitsOfWork(maxBatchSize - unitsOfWork.size())
        }

        return unitsOfWork
    }

    def countStaleRoutingUnitsOfWork(now = new DateTime()) {
        RoutingUnitOfWork.createCriteria().count {
            eq "status", RoutingUnitOfWorkStatus.IN_PROGRESS
            eq "host", host
            between "lastUpdated", now.minusHours(2).toDate(), now.minusMinutes(5).toDate()
        }
    }

    def findStaleRoutingUnitsOfWork(now = new DateTime()) {
        RoutingUnitOfWork.withCriteria {
            eq "status", RoutingUnitOfWorkStatus.IN_PROGRESS
            eq "host", host
            between "lastUpdated", now.minusHours(2).toDate(), now.minusMinutes(5).toDate()
            order "lastUpdated", "asc"
            lock true
        }
    }

    def countUnclaimedRoutingUnitsOfWork() {
        RoutingUnitOfWork.countByStatusAndDueDateLessThanEquals(RoutingUnitOfWorkStatus.PENDING, new Date())
    }

    def findUnclaimedRoutingUnitsOfWork(max) {
        RoutingUnitOfWork.findAllByStatusAndDueDateLessThanEquals(RoutingUnitOfWorkStatus.PENDING, new Date(), [ sort: "dueDate", max: max ])
    }

    def processRoutingUnitOfWork(routingUnitOfWork) {
        def expert = Expert.findByUserId(routingUnitOfWork.userId)

        if (!expert) {
            log.error "No such expert ${routingUnitOfWork.userId}"

            routingUnitOfWork.status = RoutingUnitOfWorkStatus.ERROR
            if (!routingUnitOfWork.save(flush: true)) {
                def message = "Cannot update ${routingUnitOfWork}: ${routingUnitOfWork.errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", ")}"
                log.error message
            }
            return
        }

        def firedNotification = null
        if (shouldAlert(routingUnitOfWork, expert)) {
            if (expert.questionSetting == "IMMEDIATE_EMAIL" || expert.rewardSetting ==  "IMMEDIATE_EMAIL") {
                firedNotification = fire(routingUnitOfWork, "net.insidr.routing.EmailQuestionToExpertNotification") ?: firedNotification
            }
            if (expert.questionSetting == "IMMEDIATE_SMS" || expert.rewardSetting ==  "IMMEDIATE_SMS") {
                firedNotification = fire(routingUnitOfWork, "net.insidr.routing.TxtQuestionToExpertNotification") ?: firedNotification
            }
            if (expert.hasMobileDevices) {
                firedNotification = fire(routingUnitOfWork, "net.insidr.routing.MobilePushQuestionToExpertNotification") ?: firedNotification
            }
            if (firedNotification) {
                expert.lastAlert = new Date()
                expert.save()
            }
        }
        if (!firedNotification) {
            fire(routingUnitOfWork, "net.insidr.routing.RouteQuestionToExpertNotification")
        }

        routingUnitOfWork.status = RoutingUnitOfWorkStatus.DONE
        if (!routingUnitOfWork.save(flush: true)) {
            def message = "Cannot update ${routingUnitOfWork}: ${routingUnitOfWork.errors.allErrors.collect { MessageFormat.format(it.defaultMessage, it.arguments) }.join(", ")}"
            log.error message
        }
    }

    def fire(routingUnitOfWork, notificationClassName) {
        messageBusService.sendRoutingMessage(routingUnitOfWork.userId, routingUnitOfWork.questionId, notificationClassName)
    }

    def shouldAlert(routingUnitOfWork, expert) {
        if (!routingUnitOfWork.alertRestricted) {
            return true
        }

        def now = new DateTime()
        def hoursSinceLastActive = (now.millis - (expert.lastActiveDate.getTime())) / hourInMillis
        def activityLevel = expert.activityLevel ?: 0
        def lastAlert = expert.lastAlert ?: new Date(0)

        def maximumAlertsPerHour = getMaximumAlertsPerHourFor(hoursSinceLastActive, activityLevel)
        def minimumTimeBetweenAlerts = 1 / maximumAlertsPerHour
        def hoursSinceLastAlert = (now.millis - lastAlert.getTime()) / hourInMillis
        return hoursSinceLastAlert > minimumTimeBetweenAlerts
    }

    def getMaximumAlertsPerHourFor(hoursSinceLastActive, activityLevel) {
        AlertingRestrictionRule.values.find {
            hoursSinceLastActive >= it.minimumHoursSinceLastActivity &&
                    hoursSinceLastActive <= it.maximumHoursSinceLastActivity &&
                    activityLevel >= it.minimumActivityLevel &&
                    activityLevel <= it.maximumActivityLevel
        }?.maximumAlertsPerHour ?: Double.POSITIVE_INFINITY
    }

}
