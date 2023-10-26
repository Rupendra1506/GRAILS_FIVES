package net.insidr.Tag

import net.insidr.tag.Tag

class TagService {

    def redisService

    def pruneQuestions() {
        def tagIds = redisService.smembers("${Tag.name}:tags")
        tagIds.each { Tag.pruneQuestionEntries(it) }
    }

}
