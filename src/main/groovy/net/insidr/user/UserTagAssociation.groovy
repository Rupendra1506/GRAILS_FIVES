package net.insidr.user

import grails.util.Holders
import org.apache.commons.logging.LogFactory

class UserTagAssociation {

    static final log = LogFactory.getLog(this)

    def redisService

    def internalMap = [:]

    UserTagAssociation(params = [:]) {
        try {
            redisService = Holders.grailsApplication.mainContext.getBean "redisService"
        } catch (any) {
            log.error "could not get redisService", any
        }

        internalMap << params
    }

    def getTagId() { internalMap.tagId as long }
    def getExpertId() { internalMap.expertId as long }
    def getOfficial() { Boolean.valueOf(internalMap.official) }

    static updateFrom(expertId, json) {
        def userTagAssociations = UserTagAssociation.findByExpertId(expertId)
        update(expertId, userTagAssociations, json.officialCompanies, true)
        def addedTags = update(expertId, userTagAssociations, json.unofficialCompanies + json.languages + json.certifications, false) ?: []

        def allTags = (json.unofficialCompanies + json.officialCompanies + json.languages + json.certifications).collect { it as long }

        def deletedTags = userTagAssociations.findAll { ! (it.tagId in allTags) }*.tagId

        userTagAssociations.findAll { ! (it.tagId in allTags) }.each { userTagAssociation ->
            log.info "Deleting existing ${userTagAssociation} for ${userTagAssociation.tagId} ..."

            userTagAssociation.deleteUserTagAssociation()
        }
        return [addedTags, deletedTags]
    }

    static update(expertId, userTagAssociations, tagIds, makeOffical) {
        def addedTags = []
        tagIds.each { jsonTagId ->
            def userTagAssociation = userTagAssociations.find { it.tagId == jsonTagId }
            if (userTagAssociation) {
                log.info "Update existing ${userTagAssociation} for ${jsonTagId} ..."
                userTagAssociation.internalMap.official = makeOffical as String
                userTagAssociation.save()
            } else {
                log.info "Creating new UserTagAssociation for ${jsonTagId} ..."
                new UserTagAssociation(
                        expertId: expertId as String,
                        tagId: jsonTagId as String,
                        official: makeOffical as String,
                ).save()

                addedTags << jsonTagId
            }
        }
        return addedTags
    }

    def save() {
        def id = internalMap.expertId + "_" + internalMap.tagId
        redisService.hmset("${UserTagAssociation.name}:${id}", internalMap)

        redisService.sadd("${UserTagAssociation.name}:expertId:${internalMap.expertId}", id)

        if (official) {
            redisService.sadd("${UserTagAssociation.name}:tagId:${internalMap.tagId}:official", internalMap.expertId)
            redisService.srem("${UserTagAssociation.name}:tagId:${internalMap.tagId}:unofficial", internalMap.expertId)
        } else {
            redisService.srem("${UserTagAssociation.name}:tagId:${internalMap.tagId}:official", internalMap.expertId)
            redisService.sadd("${UserTagAssociation.name}:tagId:${internalMap.tagId}:unofficial", internalMap.expertId)
        }

        return this
    }

    static def findByExpertId(expertId) {
        new UserTagAssociation().loadUserTagAssociationsbyExpertId(expertId)
    }

    static def findByTagIdAndNotOfficial(tagId) {
        new UserTagAssociation().loadUserTagAssociationsbyTagId(tagId, "unofficial")
    }

    static def findByTagIdAndOfficial(tagId) {
        new UserTagAssociation().loadUserTagAssociationsbyTagId(tagId, "official")
    }

    def loadUserTagAssociationsbyTagId(tagId, official) {
        return redisService.smembers("${UserTagAssociation.name}:tagId:${tagId}:${official}")
    }

    def loadUserTagAssociationsbyExpertId(expertId) {
        def utaIds = redisService.smembers("${UserTagAssociation.name}:expertId:${expertId}")
        utaIds.collect {
            new UserTagAssociation().loadMapByUserTagAssociationId(it)
        }
    }

    def loadMapByUserTagAssociationId(userTagAssociationId) {
        internalMap << redisService.hgetAll("${UserTagAssociation.name}:${userTagAssociationId}")
        if (internalMap.isEmpty()) {
            return null
        } else {
            return this
        }
    }

    def deleteUserTagAssociation() {
        def id = internalMap.expertId + "_" + internalMap.tagId

        redisService.srem("${UserTagAssociation.name}:expertId:${internalMap.expertId}", id)
        redisService.srem("${UserTagAssociation.name}:tagId:${internalMap.tagId}:official", internalMap.expertId)
        redisService.srem("${UserTagAssociation.name}:tagId:${internalMap.tagId}:unofficial", internalMap.expertId)
    }

}
