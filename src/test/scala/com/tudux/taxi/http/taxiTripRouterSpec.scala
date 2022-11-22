package com.tudux.taxi.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.GivenWhenThen
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.service.ServiceActor
import com.tudux.taxi.app.ShardedActorsGenerator._
import com.tudux.taxi.http.payloads.RoutePayloads.CreateTaxiTripRequest
import com.tudux.taxi.http.routes.MainRouter

class taxiTripRouterSpec extends AnyFeatureSpecLike  with GivenWhenThen with ScalatestRouteTest
  with SprayJsonSupport{

  info("As a user of the application")
  info("I should be able to handle Taxi Trip information")
  info("So I should be able to use the resources available to create and delete taxi trip")

  // Create the aggregators
  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator")
    , "cost-aggregator")
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator")
    , "time-aggregator")

  // Create the sharded version of the persistent actors
  val persistentCostShardRegionRef: ActorRef = createShardedCostActor(system, costAggregatorActor)
  val persistentExtraInfoShardedRegionRef: ActorRef = createShardedExtraInfoActor(system)
  val persistentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val persistentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system, timeAggregatorActor)

  // Specific Service actor
  val serviceActor: ActorRef = system.actorOf(ServiceActor.props(costAggregatorActor, timeAggregatorActor),
    "serviceActor")

  //routes
  val router = new MainRouter(persistentCostShardRegionRef, persistentExtraInfoShardedRegionRef,
    persistentPassengerShardRegionRef, persistentTimeInfoShardRegionRef, serviceActor)

  Feature("Handle taxi trip endpoints") {
    Scenario("Create a new taxi trip") {
      Given("A taxi trip create request")
        val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
          tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
          passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
          storeAndFwdFlag = "N", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
          fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
          totalAmount = 2.0)

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest)
    }

  }

}
