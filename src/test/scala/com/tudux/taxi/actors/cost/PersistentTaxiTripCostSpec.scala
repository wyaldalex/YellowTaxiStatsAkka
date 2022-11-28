package com.tudux.taxi.actors.cost

import akka.actor.{ActorSystem, Kill}
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
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val APersistentCostStatsAggregatorActor = system.actorOf(PersistentCostStatsAggregator.props
      ("cost-aggregator-test"))
      val APersistentTaxiTripCostActor = system.actorOf(PersistentTaxiTripCost.props
      (APersistentCostStatsAggregatorActor))

      val taxiTripCost: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)

      val createCommand = TaxiTripCostCommand.CreateTaxiTripCost(tripId, taxiTripCost)
      APersistentTaxiTripCostActor ! createCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      When("A restart happens")
      APersistentTaxiTripCostActor ! Kill

      Then("PersistentTaxiExtraInfo should restore his state")
      val ARestartedPersistentTaxiTripCostActor = system.actorOf(PersistentTaxiTripCost.props
      (APersistentCostStatsAggregatorActor))

      val getCommand = TaxiTripCostCommand.GetTaxiTripCost(tripId)
      ARestartedPersistentTaxiTripCostActor ! getCommand
      expectMsg(taxiTripCost)
    }
  }

}
