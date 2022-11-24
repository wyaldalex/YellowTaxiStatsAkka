package com.tudux.taxi.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.{PersistentTaxiTripCost, TaxiTripCost}
import com.tudux.taxi.actors.extrainfo.PersistentTaxiExtraInfo
import com.tudux.taxi.actors.passenger.PersistentTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.PersistentTaxiTripTimeInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.formatters.RouteFormatters.TaxiCostStatProtocol
import com.tudux.taxi.http.routes.{CommonTaxiTripRoutes, CostRoutes}
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

import scala.concurrent.duration._

class CostRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with ScalatestRouteTest
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with TaxiCostStatProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip Cost information")
  info("So I should be able to use the resources available to get and update taxi trip cost")
  // Initializing timers
  implicit val timeoutRouteTestTimeout = RouteTestTimeout(60.seconds.dilated)
  implicit val timeout: Timeout = Timeout(30.seconds)

  // Create the aggregators
  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator")
    , "cost-aggregator")
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator")
    , "time-aggregator")

  // Create the persistent actors
  val persistentExtraInfo: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
  val persistentPassenger: ActorRef = system.actorOf(PersistentTaxiTripPassengerInfo.props)
  val persistentTimeInfo: ActorRef = system.actorOf(PersistentTaxiTripTimeInfo.props(timeAggregatorActor))

  // Create the persistent actors
  val persistentCost: ActorRef = system.actorOf(PersistentTaxiTripCost.props(costAggregatorActor))

  //routes
  val commonRoutes = CommonTaxiTripRoutes(persistentCost,
    persistentExtraInfo, persistentPassenger, persistentTimeInfo).routes
  val costRoutes = CostRoutes(persistentCost).routes
  val aTaxiTripCost: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
    fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
    totalAmount = 2.0)
  // initializing variables
  var taxiTripId: String = ""

  override def beforeEach() {
    val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
      tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
      passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
      storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
      fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
      totalAmount = 2.0)

    Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> commonRoutes ~> check {
      taxiTripId = entityAs[CombinedTaxiTripOperationResponse].costResponse.id
    }
    super.beforeEach()
  }

  override def afterEach() {
    try super.afterEach()
    finally Delete(s"/api/yellowtaxi/taxitrip/$taxiTripId") ~> commonRoutes
  }

  Feature("Handle get taxi trip cost endpoint") {

    Scenario("Get cost existent taxi trip id") {
      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/cost/$taxiTripId") ~> costRoutes ~> check {

        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripCost] shouldBe aTaxiTripCost
      }
    }

    Scenario("Get cost with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/cost/$aNotExistentTaxiTripId") ~> costRoutes ~> check {
        Then("should response with a NotFound")
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
