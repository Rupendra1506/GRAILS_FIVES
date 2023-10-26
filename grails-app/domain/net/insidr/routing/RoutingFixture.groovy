package net.insidr.routing

import net.insidr.user.MetricTiers
import net.insidr.user.OnlineStatus
import net.insidr.user.groups.TierOnlineStatusExpertGroup

class RoutingFixture {

    def tagId

    def random = new Random()

    def onlineTier1Group
    def onlineTier2Group
    def offlineTier1Group
    def offlineTier2Group

    def onlineTier1Classification
    def onlineTier2Classification
    def offlineTier1Classification
    def offlineTier2Classification

    RoutingFixture(companyTagId) {
        tagId = companyTagId

        onlineTier1Group = buildRoutingGroup(OnlineStatus.online, MetricTiers.TIER_1, tagId)
        onlineTier2Group = buildRoutingGroup(OnlineStatus.online, MetricTiers.TIER_2, tagId)
        offlineTier1Group = buildRoutingGroup(OnlineStatus.offline, MetricTiers.TIER_1, tagId)
        offlineTier2Group = buildRoutingGroup(OnlineStatus.offline, MetricTiers.TIER_2, tagId)

        onlineTier1Classification = "${tagId}:${MetricTiers.TIER_1}:${OnlineStatus.online}"
        onlineTier2Classification = "${tagId}:${MetricTiers.TIER_2}:${OnlineStatus.online}"
        offlineTier1Classification = "${tagId}:${MetricTiers.TIER_1}:${OnlineStatus.offline}"
        offlineTier2Classification = "${tagId}:${MetricTiers.TIER_2}:${OnlineStatus.offline}"
    }

    def buildRoutingGroup(onlineStatus, tier, tagId) {
        def group = new TierOnlineStatusExpertGroup(onlineStatus, tier, tagId)
        populateGroup(group)
        return group
    }

    def getRandomId() {
        random.nextInt()
    }

    private populateGroup(group) {
        group.add(randomId)
        group.add(randomId)
        group.add(randomId)
        group.add(randomId)
    }

    def getExperts() {
        return (onlineTier1Group.experts() + onlineTier2Group.experts() + offlineTier1Group.experts() + offlineTier2Group.experts()) as List
    }

    def getExperts(OnlineStatus status, MetricTiers tier) {
        return new TierOnlineStatusExpertGroup(status, tier, tagId).experts()
    }
}
