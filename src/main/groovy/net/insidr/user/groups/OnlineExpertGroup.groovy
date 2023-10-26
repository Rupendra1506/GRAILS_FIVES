package net.insidr.user.groups

import net.insidr.util.RedisKeys

class OnlineExpertGroup extends ExpertGroup {
    OnlineExpertGroup() {
        super(RedisKeys.onlineExpertsKey)
    }
}
