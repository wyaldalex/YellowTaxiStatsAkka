package com.tudux.taxi.actors.aggegators

import akka.actor.{ActorSystem, Kill}
import akka.testkit.{ImplicitSender, TestKit}
import com.tudux.taxi.actors.aggregators.{AggregatorStat, CostAggregatorCommand, CostAggregatorResponse,
  PersistentCostStatsAggregator}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class PersistentCostStatsAggregatorSpec extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {
  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentCostStatsAggregator actor and commands")
      val aPersistentCostStatsAggregator = system.actorOf(PersistentCostStatsAggregator.props(
        "persistent-cost-stats-aggregator"))

      // Auxiliary variables for commands
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val stat = AggregatorStat(10, 10, 1)

      // Add command
      val addCommand = CostAggregatorCommand.AddCostAggregatorValues(tripId, stat)
      // Update command
      val updateCommand = CostAggregatorCommand.UpdateCostAggregatorValues(tripId = tripId, totalAmountDelta =
        5, distanceDelta = 5, tipAmountDelta = 0.5, tipAmount = 2)
      // get command
      val getCommand = CostAggregatorCommand.GetAverageTipAmount

      // expected averageTipAmount
      val expectedResponse = CostAggregatorResponse.GetAverageTipAmountResponse(1.5)

      // Sending add command
      aPersistentCostStatsAggregator ! addCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending update command
      aPersistentCostStatsAggregator ! updateCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Confirming response
      aPersistentCostStatsAggregator ! getCommand
      expectMsg(expectedResponse)

      When("A restart happens")
      aPersistentCostStatsAggregator ! Kill

      Then("PersistentCostStatsAggregator should restore his state")
      val aRestartedPersistentCostStatsAggregator = system.actorOf(PersistentCostStatsAggregator.props(
        "persistent-cost-stats-aggregator"))

      aRestartedPersistentCostStatsAggregator ! getCommand
      expectMsg(expectedResponse)
    }
  }

}
