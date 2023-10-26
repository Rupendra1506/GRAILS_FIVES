package net.insidr.user.groups

import net.insidr.util.RedisUtil
import org.apache.commons.logging.LogFactory

class ExpertGroup {
    static final log = LogFactory.getLog(this)

    def redisService

    def key

    ExpertGroup(key) {
        redisService = RedisUtil.redisService
        this.key = key
    }

    def experts() {
        return redisService.smembers(key) as Set
    }

    def replace(expertIds) {
        RedisUtil.replaceSetTx(key, expertIds)
    }

    def add(expertId) {
        redisService.sadd(key, expertId as String)
    }

    def delete(expertId) {
        redisService.srem(key, expertId as String)
    }

    def count() {
        redisService.scard(key)
    }
}

