package net.insidr.internal.status

import com.budjb.rabbitmq.RunningState
import groovy.sql.Sql
import net.insidr.routing.RoutingUnitOfWork
import net.insidr.routing.RoutingUnitOfWorkStatus
import org.joda.time.DateTime

class StatusService {

    def dataSource
    def rabbitContext
    def redisService

    def current() {
        def internalStatus = new InternalStatus()

        internalStatus.add(databaseStatus())
        internalStatus.add(rabbitmqStatus())
        internalStatus.add(redisStatus())
        internalStatus.add(routingUnitsOfWorkStatus())
        internalStatus.updateGeneralStatus()

        return internalStatus
    }

    def databaseStatus() {
        try {
            // we need to unrwrap this twice since it's a LazyConnectionDataSourceProxy wrapped in a TransactionAwareDataSourceProxy
            def unwrappedDataSource = dataSource.getTargetDataSource().getTargetDataSource()
            def version = new Sql(dataSource).firstRow("SELECT SUBSTRING(FILENAME FROM 1 FOR LOCATE('/', FILENAME) - 1) AS version FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED DESC").version
            if (version) {
                return new InternalStatusEntry(InternalStatusCheckItem.DATABASE_CONNECTION, InternalStatusCode.OK, "Current schema version is ${version}.  dataSource properties: ${unwrappedDataSource.maxActive} maxActive, ${unwrappedDataSource.maxIdle} maxIdle, ${unwrappedDataSource.minIdle} minIdle, ${unwrappedDataSource.initialSize} initialSize, ${unwrappedDataSource.maxWait} maxWait.")
            } else {
                return new InternalStatusEntry(InternalStatusCheckItem.DATABASE_CONNECTION, InternalStatusCode.FAILING, "Couldn't load data from database.")
            }
        } catch (any) {
            return new InternalStatusEntry(InternalStatusCheckItem.DATABASE_CONNECTION, InternalStatusCode.CRITICAL, "Exception connecting to database ${any.message}")
        }
    }

    def rabbitmqStatus() {
        try {
            def consumers = rabbitContext.statusReport.consumers.first()
            def runningStateText = consumers.collect { "${it.name} (${it.runningState})" }.join(", ")
            if (consumers.every { it.runningState == RunningState.RUNNING }) {
                return new InternalStatusEntry(InternalStatusCheckItem.RABBITMQ_CONNECTION, InternalStatusCode.OK, runningStateText)
            } else {
                return new InternalStatusEntry(InternalStatusCheckItem.RABBITMQ_CONNECTION, InternalStatusCode.FAILING, runningStateText)
            }
        } catch (any) {
            return new InternalStatusEntry(InternalStatusCheckItem.RABBITMQ_CONNECTION, InternalStatusCode.CRITICAL, "rabbitmqService failed with exception \"${any.message}\".")
        }
    }

    def redisStatus() {
        try {
            def redisCheck = redisService.ping()
            if (redisCheck == "PONG") {
                return new InternalStatusEntry(InternalStatusCheckItem.REDIS_CONNECTION, InternalStatusCode.OK)
            } else {
                return new InternalStatusEntry(InternalStatusCheckItem.REDIS_CONNECTION, InternalStatusCode.FAILING, "test on redisService was \"${redisCheck}\", was expecting \"PONG\".")
            }
        } catch (any) {
            return new InternalStatusEntry(InternalStatusCheckItem.REDIS_CONNECTION, InternalStatusCode.CRITICAL, "redisService failed with exception \"${any.message}\".")
        }
    }

    def routingUnitsOfWorkStatus() {
        try {
            def fifteenMinutesAgo = new DateTime().minusMinutes(15)
            def numPending = RoutingUnitOfWork.countByStatusAndDueDateGreaterThan(RoutingUnitOfWorkStatus.PENDING, fifteenMinutesAgo)
            def numInProgress = RoutingUnitOfWork.countByStatusAndDueDateGreaterThan(RoutingUnitOfWorkStatus.IN_PROGRESS, fifteenMinutesAgo)
            def numClaimed = RoutingUnitOfWork.countByStatusAndDueDateGreaterThan(RoutingUnitOfWorkStatus.CLAIMED, fifteenMinutesAgo)
            def metrics = [
                numPendingRoutingUnitsOfWork: "${numPending}",
                numClaimedRoutingUnitsOfWork: "${numClaimed}",
                numInProgressRoutingUnitsOfWork: "${numInProgress}",
            ]
            return new InternalStatusEntry(
                InternalStatusCheckItem.ROUTING_UNITS_OF_WORK,
                InternalStatusCode.OK,
                "Number of PENDING (unrouted) routing units of work is ${numPending}, Number of CLAIMED routing units of work is ${numClaimed}, Number of IN_PROGRESS (stale) routing units of work is ${numInProgress}",
                metrics
            )

        } catch (any) {
            return new InternalStatusEntry(
                InternalStatusCheckItem.ROUTING_UNITS_OF_WORK,
                InternalStatusCode.FAILING,
                "Exception querying for the number of routing units of work in database ${any.message}"
            )
        }
    }

}
