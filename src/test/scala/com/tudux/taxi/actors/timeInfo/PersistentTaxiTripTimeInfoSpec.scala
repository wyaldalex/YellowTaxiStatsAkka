package com.tudux.taxi.actors.timeInfo

import akka.actor.{ActorSystem, Kill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.tudux.taxi.actors.aggregators.PersistentTimeStatsAggregator
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.timeinfo.{PersistentTaxiTripTimeInfo, TaxiTripTimeInfo, TaxiTripTimeInfoCommand}
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class PersistentTaxiTripTimeInfoSpec extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {

  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentTaxiTripTimeInfo actor and commands")
      // Creating Actor
      val aPersistentTimeStatsAggregatorActor = system.actorOf(PersistentTimeStatsAggregator.props(
        "persistent-time-stats-aggregator-test-id"))
      val aPersistentTaxiTripTimeInfoActor = system.actorOf(Props(
        new PersistentTaxiTripTimeInfo(aPersistentTimeStatsAggregatorActor) {
          override def persistenceId: String = "persistent-taxi-extra-info-test-id"
        }))

      // Auxiliary variables for commands
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val time1: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 19:05:42",
        tpepDropoffDatetime = "2015-01-15 20:06:43")
      val time2: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 20:05:42",
        tpepDropoffDatetime = "2015-01-15 20:16:43")

      // createCommand
      val createCommand = TaxiTripTimeInfoCommand.CreateTaxiTripTimeInfo(tripId, time1)
      // updateCommand
      val updateCommand = TaxiTripTimeInfoCommand.UpdateTaxiTripTimeInfo(tripId, time2)
      // delete command
      val deleteCommand = TaxiTripTimeInfoCommand.DeleteTaxiTripTimeInfo(tripId)

      // get command
      val getCommand = TaxiTripTimeInfoCommand.GetTaxiTripTimeInfo(tripId)

      // Sending create command
      aPersistentTaxiTripTimeInfoActor ! createCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending update command
      aPersistentTaxiTripTimeInfoActor ! updateCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending delete command
      aPersistentTaxiTripTimeInfoActor ! deleteCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      When("A restart happens")
      aPersistentTaxiTripTimeInfoActor ! Kill

      Then("PersistentTaxiExtraInfo should restore his state")
      val aRestartedPersistentTaxiTripTimeInfoActor = system.actorOf(Props(
        new PersistentTaxiTripTimeInfo(aPersistentTimeStatsAggregatorActor) {
          override def persistenceId: String = "persistent-taxi-extra-info-test-id"
        }))

      aRestartedPersistentTaxiTripTimeInfoActor ! getCommand
      expectMsg(time2.copy(deletedFlag = true))
    }
  }
}
