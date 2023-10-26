package net.insidr.api.model

import io.swagger.annotations.ApiModel
import net.insidr.user.ExpertReputation
import net.insidr.user.MetricTiers

@ApiModel
class ExpertReputationList {
    List<ExpertReputation> expertReputations
    MetricTiers metricTiers
    def getOverallReputation() {
        if (expertReputations.size() == 0) {
            return MetricTiers.NA.name()
        }

        def minRep = expertReputations.min { a, b ->
            def aTier = a.overallReputationForCompany
            def bTier = b.overallReputationForCompany
            if (aTier == MetricTiers.NA.name()) {
                return 1
            }
            if (bTier == MetricTiers.NA.name()) {
                return -1
            }
            return MetricTiers[aTier].displayPercentage <=> MetricTiers[bTier].displayPercentage
        }
        return minRep.overallReputationForCompany
    }
}

