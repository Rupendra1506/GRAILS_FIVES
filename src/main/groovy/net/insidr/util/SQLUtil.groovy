package net.insidr.util

import groovy.sql.Sql

class SQLUtil {

    static def acquireRoutingLock(dataSource) {
        new Sql(dataSource).firstRow("SELECT GET_LOCK('routing_unit_of_work_lock', 1)")[0]
    }

    static def releaseRoutingLock(dataSource) {
        new Sql(dataSource).firstRow("SELECT RELEASE_LOCK('routing_unit_of_work_lock')")[0]
    }

}
