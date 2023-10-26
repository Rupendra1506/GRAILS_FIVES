package net.insidr.util

import grails.util.Holders
import org.apache.commons.logging.LogFactory
import redis.clients.jedis.Transaction

class RedisUtil {
    static final log = LogFactory.getLog(this)

    static getRedisService() {
        try {
            return Holders.grailsApplication.mainContext.getBean("redisService")
        } catch (any) {
            log.error "could not get redisService", any
        }
    }

    /**
     * Completely replaces a redis set, but as a transaction so that the
     * the set exists at all times (never a moment where it can't be found)
     */
    static def replaceSetTx(key, newValues) {
        redisService.withTransaction { Transaction tx ->
            tx.del(key)
            newValues.each { tx.sadd(key, it as String) }
        }
    }
}
