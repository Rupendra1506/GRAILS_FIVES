package net.insidr.routing

import net.insidr.company.Company
import net.insidr.question.Question

class RoutingInstructions {
    Question question
    RoutingBatch[] routingPlan = []
    Company company
    double targetPercentage
    String routingTier
    int questionsPerHour
    int totalRoutedExperts
    int totalRoutableExperts
    Double percentOfExpertsPerQuestion
}
