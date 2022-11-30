package com.tudux.taxi.actors.cost

import akka.actor.{ActorSystem, Kill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.tudux.taxi.actors.aggregators.PersistentCostStatsAggregator
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers


class PersistentTaxiTripCostSpec extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {
  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentTaxiExtraInfo actor")
      // Creating Actor
      val aPersistentCostStatsAggregatorActor = system.actorOf(PersistentCostStatsAggregator.props
      ("cost-aggregator-test"))
      val aPersistentTaxiTripCostActor = system.actorOf(Props(
        new PersistentTaxiTripCost(aPersistentCostStatsAggregatorActor) {
        override def persistenceId: String = "persistent-taxi-trip-cost-test-id"
      }))

      // Auxiliary variables for commands
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val createInfo: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)
      val updateInfo: TaxiTripCost = TaxiTripCost(vendorID = 2, tripDistance = 2.53, paymentType = 3,
        fareAmount = 19, extra = 0.5, mtaTax = 1, tipAmount = 1, tollsAmount = 2, improvementSurcharge = 5,
        totalAmount = 3.0)

      // Create command
      val createCommand = TaxiTripCostCommand.CreateTaxiTripCost(tripId, createInfo)
      // Update command
      val updateCommand = TaxiTripCostCommand.UpdateTaxiTripCost(tripId, updateInfo)
      // Delete command
      val deleteCommand = TaxiTripCostCommand.DeleteTaxiTripCost(tripId)

      // Get command
      val getCommand = TaxiTripCostCommand.GetTaxiTripCost(tripId)

      // Sending create command
      aPersistentTaxiTripCostActor ! createCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending update command
      aPersistentTaxiTripCostActor ! updateCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending delete command
      aPersistentTaxiTripCostActor ! deleteCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      When("A restart happens")
      aPersistentTaxiTripCostActor ! Kill

      Then("PersistentTaxiExtraInfo should restore his state")
      val aRestartedPersistentTaxiTripCostActor = system.actorOf(Props(
        new PersistentTaxiTripCost(aPersistentCostStatsAggregatorActor) {
          override def persistenceId: String = "persistent-taxi-trip-cost-test-id"
        }))

      aRestartedPersistentTaxiTripCostActor ! getCommand
      expectMsg(updateInfo.copy(deletedFlag = true))
    }
  }

}
