package net.insidr.user

import net.insidr.company.CompanyQuestionsSchedule
import net.insidr.routing.NotificationConsumer
import net.insidr.user.groups.OnlineExpertGroup

class OnlineUsersNotificationConsumer extends NotificationConsumer {

    def companyService
    def expertGroupService
    def tagService

    // We have to repeat the configuration here otherwise the consumer won't start.
    static rabbitConfig = [
            queue: "directly.notifications.andre.onlineUsers",
            retry: true,
    ]

    def handle(body) {
        log.info("START - OnlineUsersNotificationConsumer")
        new OnlineExpertGroup().replace(body.onlineUsers)

        // Pruning old questions to keep question volume as in sync with experts online as possible
        companyService.pruneQuestions()
        tagService.pruneQuestions()

        // Prune to recent entries for each company
        expertGroupService.pruneExpertNegativeFeedback()

        expertGroupService.buildBaseExpertGroupsForAllCompanies()

        if(body.companyExpertOpenQuestions){
            CompanyQuestionsSchedule.updateExpertOpenQuestions(body.companyExpertOpenQuestions)
        }

        log.info("END - OnlineUsersNotificationConsumer")
    }

}
