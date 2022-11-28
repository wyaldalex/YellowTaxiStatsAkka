package com.tudux.taxi.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.CostAggregatorResponse.{CalculateTripDistanceCostResponse,
  GetAverageTipAmountResponse}
import com.tudux.taxi.actors.aggregators.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.PersistentTaxiTripCost
import com.tudux.taxi.actors.extrainfo.PersistentTaxiExtraInfo
import com.tudux.taxi.actors.passenger.PersistentTaxiTripPassengerInfo
import com.tudux.taxi.actors.service.ServiceActor
import com.tudux.taxi.actors.timeinfo.PersistentTaxiTripTimeInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.formatters.RouteFormatters.{CalculateAverageTripTimeProtocol,
  CalculateDistanceCostProtocol, GetAverageTipAmountProtocol}
import com.tudux.taxi.http.routes.{CommonTaxiTripRoutes, ServiceRoutes}
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

import scala.concurrent.duration._

class ServiceRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with ScalatestRouteTest
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with
  CalculateDistanceCostProtocol
  with CalculateAverageTripTimeProtocol with GetAverageTipAmountProtocol {

  info("As a user of the application")
  info("I should be able to handle Services information")
  info("So I should be able to use the resources available to get services")

  // Initializing timers
  implicit val timeoutRouteTestTimeout = RouteTestTimeout(60.seconds.dilated)
  implicit val timeout: Timeout = Timeout(30.seconds)

  // Create the aggregators
  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator")
    , "cost-aggregator")
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator")
    , "time-aggregator")

  // Create the persistent actors
  val persistentCost: ActorRef = system.actorOf(PersistentTaxiTripCost.props(costAggregatorActor))
  val persistentExtraInfo: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
  val persistentPassenger: ActorRef = system.actorOf(PersistentTaxiTripPassengerInfo.props)
  val persistentTimeInfo: ActorRef = system.actorOf(PersistentTaxiTripTimeInfo.props(timeAggregatorActor))

  // Specific Service actor
  val serviceActor: ActorRef = system.actorOf(ServiceActor.props(costAggregatorActor, timeAggregatorActor),
    "serviceActor")

  //routes
  val commonRoutes = CommonTaxiTripRoutes(persistentCost,
    persistentExtraInfo, persistentPassenger, persistentTimeInfo).routes

  val servicesRoutes = ServiceRoutes(serviceActor).routes

  // initializing variables
  var taxiTripIdOne: String = ""
  var taxiTripIdTwo: String = ""

  override def beforeEach() {
    val aCreateTaxiTripRequestOne: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
      tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
      passengerCount = 1, tripDistance = 1.53, pickupLongitude = 19.79344, pickupLatitude = -70.6884,
      rateCodeID = 1, storeAndFwdFlag = "Y", dropoffLongitude = 19.4517, dropoffLatitude = -70.69703,
      paymentType = 2, fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 1, tollsAmount = 0,
      improvementSurcharge = 0, totalAmount = 10.0)

    val aCreateTaxiTripRequestTwo: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 2,
      tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 23:16:18",
      passengerCount = 3, tripDistance = 2.53, pickupLongitude = 19.79344, pickupLatitude = -70.6884,
      rateCodeID = 1, storeAndFwdFlag = "Y", dropoffLongitude = 18.47186, dropoffLatitude = -69.89232,
      paymentType = 3, fareAmount = 9, extra = 1, mtaTax = 0.5, tipAmount = 2, tollsAmount = 1,
      improvementSurcharge = 0.5, totalAmount = 25.0)

    Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequestOne) ~> commonRoutes ~> check {
      taxiTripIdOne = entityAs[CombinedTaxiTripOperationResponse].costResponse.id
    }

    Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequestTwo) ~> commonRoutes ~> check {
      taxiTripIdTwo = entityAs[CombinedTaxiTripOperationResponse].costResponse.id
    }

    super.beforeEach()
  }

  override def afterEach() {
    try super.afterEach()
    finally {
      Delete(s"/api/yellowtaxi/taxitrip/$taxiTripIdOne") ~> commonRoutes
      Delete(s"/api/yellowtaxi/taxitrip/$taxiTripIdTwo") ~> commonRoutes
    }
  }

  Feature("Handle get taxi trip estimate cost base on distance endpoint") {

    Scenario("Get taxi trip estimate cost base on distance") {
      Given("Distance and expected estimate cost")
      val distance: Int = 10
      val expectedEstimateCost: Double = 13.071895424836601

      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/service/calculate-distance-cost/$distance") ~> servicesRoutes ~> check {

        Then("should response with a OK status code AND estimatedCost should be equal to " +
          "expectedEstimateCost")
        status shouldBe StatusCodes.OK
        entityAs[CalculateTripDistanceCostResponse].estimatedCost shouldBe expectedEstimateCost
      }
    }

  }

  Feature("Handle get average trip time endpoint") {

    Scenario("Get taxi trip average trip time") {
      Given("Distance and expected estimate cost")
      val expectedAverageTime: Double = 130.0

      When("a user send a GET request to get the taxi trip average trip time")
      Get(s"/api/yellowtaxi/service/average-trip-time") ~> servicesRoutes ~> check {

        Then("should response with a OK status code AND averageTimeMinutes should be equal to " +
          "expectedAverageTime")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripAverageTimeMinutesResponse].averageTimeMinutes shouldBe expectedAverageTime
      }
    }
  }

  Feature("Handle get average tip amount endpoint") {

    Scenario("Get taxi trip average trip time") {
      Given("Distance and expected estimate cost")
      val expectedAverageTipAmount: Double = 1.5

      When("a user send a GET request to get the taxi trip average tip amount")
      Get(s"/api/yellowtaxi/service/average-tip-amount") ~> servicesRoutes ~> check {

        Then("should response with a OK status code AND averageTipAmount should be equal to " +
          "expectedAverageTipAmount")
        status shouldBe StatusCodes.OK
        entityAs[GetAverageTipAmountResponse].averageTipAmount shouldBe expectedAverageTipAmount
      }
    }
  }


}
