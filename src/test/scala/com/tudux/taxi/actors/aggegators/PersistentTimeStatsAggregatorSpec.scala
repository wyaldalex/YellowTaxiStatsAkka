package com.tudux.taxi.actors.aggegators

import akka.actor.{ActorSystem, Kill}
import akka.testkit.{ImplicitSender, TestKit}
import com.tudux.taxi.actors.aggregators.{PersistentTimeStatsAggregator, TimeAggregatorCommand,
  TimeAggregatorResponse}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class PersistentTimeStatsAggregatorSpec extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {
  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentTimeStatsAggregator actor and commands")
      val aPersistentTimeStatsAggregator = system.actorOf(PersistentTimeStatsAggregator.props(
        "persistent-time-stats-aggregator"))

      // Auxiliary variables for commands
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val time1: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 19:05:42",
        tpepDropoffDatetime = "2015-01-15 20:06:43")
      val time2: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 20:05:42",
        tpepDropoffDatetime = "2015-01-15 20:16:43")

      // Add command
      val addCommand = TimeAggregatorCommand.AddTimeAggregatorValues(tripId, time1)
      // Update command
      val updateCommand = TimeAggregatorCommand.UpdateTimeAggregatorValues(tripId = tripId, preTime = time1,
        newTime = time2)
      // get command
      val getCommand = TimeAggregatorCommand.GetAverageTripTime

      // expected averageTipAmount
      val expectedAverageTime = TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse(11)

      // sending add command
      aPersistentTimeStatsAggregator ! addCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // sending update command
      aPersistentTimeStatsAggregator ! updateCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Confirming response
      aPersistentTimeStatsAggregator ! getCommand
      expectMsg(expectedAverageTime)

      When("A restart happens")
      aPersistentTimeStatsAggregator ! Kill

      Then("PersistentTimeStatsAggregator should restore his state")
      val aRestartedPersistentTimeStatsAggregator = system.actorOf(PersistentTimeStatsAggregator.props(
        "persistent-time-stats-aggregator"))

      aRestartedPersistentTimeStatsAggregator ! getCommand
      expectMsg(expectedAverageTime)
    }
  }

}
