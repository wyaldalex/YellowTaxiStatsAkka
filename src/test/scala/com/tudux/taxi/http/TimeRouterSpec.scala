package com.tudux.taxi.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.{PersistentTaxiTripCost, TaxiTripCost}
import com.tudux.taxi.actors.extrainfo.PersistentTaxiExtraInfo
import com.tudux.taxi.actors.passenger.PersistentTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.{PersistentTaxiTripTimeInfo, TaxiTripTimeInfo}
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.formatters.RouteFormatters.{TaxiCostStatProtocol, TaxiTimeInfoStatProtocol}
import com.tudux.taxi.http.routes.{CommonTaxiTripRoutes, CostRoutes, TimeRoutes}
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

import scala.concurrent.duration._

class TimeRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with ScalatestRouteTest
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with TaxiTimeInfoStatProtocol {

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
  val persistentCost: ActorRef = system.actorOf(PersistentTaxiTripCost.props(costAggregatorActor))
  val persistentExtraInfo: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
  val persistentPassenger: ActorRef = system.actorOf(PersistentTaxiTripPassengerInfo.props)
  val persistentTimeInfo: ActorRef = system.actorOf(PersistentTaxiTripTimeInfo.props(timeAggregatorActor))

  //routes
  val commonRoutes = CommonTaxiTripRoutes(persistentCost,
    persistentExtraInfo, persistentPassenger, persistentTimeInfo).routes
  val timeRoutes = TimeRoutes(persistentTimeInfo).routes

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

  Feature("Handle get taxi trip time endpoint") {

    Scenario("Get time existent taxi trip id") {
      Given("a TaxiTripCost to be got it")
      val aTaxiTripTimeInfo: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 19:05:42",
        tpepDropoffDatetime = "2015-01-15 19:16:18")

      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/time/$taxiTripId") ~> timeRoutes ~> check {


        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripTimeInfo] shouldBe aTaxiTripTimeInfo
      }
    }

    Scenario("Get cost with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/time/$aNotExistentTaxiTripId") ~> timeRoutes ~> check {
        Then("should response with a NotFound")
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  Feature("Handle put taxi trip time endpoint") {

    Scenario("Update a taxi trip test case #1") {
      Given("A taxi trip time update request")
      val aTaxiTripTimeInfo: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 19:05:42",
        tpepDropoffDatetime = "2015-01-15 19:16:18")

      When("a user send a Put request to update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId", aTaxiTripTimeInfo) ~> timeRoutes ~> check {
        Then("should response with an Created status code")
        status shouldBe StatusCodes.Created
      }
    }

    Scenario("Update a taxi trip time test case #7") {
      Given("A taxi trip update request with empty tpepPickupDatetime")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18"
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip time test case #8") {
      Given("A taxi trip update request with empty tpepDropoffDatetime")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": ""
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip time test case #9") {
      Given("A taxi trip update request with a invalid pickup date")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "2015-13-33 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18"
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip time test case #10") {
      Given("A taxi trip update request with a invalid dropoff date")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-13-33 19:05:42"
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip time test case #11") {
      Given("A taxi trip update request with a invalid pickup and dropoff date")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "2015-13-33 19:05:42",
           |  "tpepDropoffDatetime": "2015-12-23 19:05:42"
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip time test case #12") {
      Given("A taxi trip update request with pickup date older than dropoff")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "tpepPickupDatetime": "2015-01-15 20:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18"
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip time")
      Put(s"/api/yellowtaxi/time/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> timeRoutes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

}
