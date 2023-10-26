package net.insidr.tag

import net.insidr.util.RedisKeys
import net.insidr.util.RedisUtil

class Tag {

    static final EPOCH_START_TIME = 0

    def redisService

    def internalMap = [:]

    Tag(params = [:]) {
        redisService = RedisUtil.redisService
        internalMap << params
    }

    def getTagId() { internalMap.tagId as String }

    static addQuestionToTag(question, tagId) {
        new Tag(tagId:tagId).addQuestionByTagId(question)
    }

    def addQuestionByTagId(question) {
        redisService.zadd(RedisKeys.buildTagQuestionsKey(tagId), question.dateCreated.time as Double, question.questionId as String)
        redisService.sadd("${Tag.name}:tags", tagId)
    }

    static pruneQuestionEntries(tagId, lookbackInMinutes=15) {
       new Tag(tagId: tagId).pruneQuestions(lookbackInMinutes)
    }

    def pruneQuestions(lookbackInMinutes=15) {
        def endTime = new Date().time - (lookbackInMinutes * 60 * 1000)
        redisService.zremrangeByScore(RedisKeys.buildTagQuestionsKey(tagId), EPOCH_START_TIME, endTime)
    }

}

