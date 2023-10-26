package net.insidr.user.groups

import net.insidr.util.DateFormatter
import net.insidr.util.RedisKeys
import redis.clients.jedis.Transaction

class NegativeFeedbackExpertGroup extends CompanyExpertGroup {
    static final EPOCH_START_TIME = 0
    static final MAX_EXPERT_NEGATIVE_FEEDBACK_HOURS = 24

    NegativeFeedbackExpertGroup(companyTagId) {
        super(companyTagId, RedisKeys.buildExpertNegativeFeedbackKey(companyTagId))
    }

    @Override
    def experts() {
        return redisService.zrangeByScore(key, EPOCH_START_TIME, MAX_EXPERT_NEGATIVE_FEEDBACK_HOURS)
    }

    @Override
    def replace(expertIds) {
        redisService.withTransaction { Transaction tx ->
            tx.del(key)
            expertIds.each { tx.zadd(key, DateFormatter.currentTime(), it as String) }
        }
    }

    @Override
    def add(expertId) {
        redisService.zadd(key, DateFormatter.currentTime(), expertId as String)
    }

    def prune(lookbackInHours=MAX_EXPERT_NEGATIVE_FEEDBACK_HOURS) {
        def endTime = new Date().time - (lookbackInHours * 60 * 60 * 1000)
        def key = RedisKeys.buildExpertNegativeFeedbackKey(companyTagId)
        redisService.zremrangeByScore(key, EPOCH_START_TIME, endTime)
    }
}
