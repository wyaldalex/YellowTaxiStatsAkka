package com.tudux.taxi.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.PersistentTaxiTripCost
import com.tudux.taxi.actors.extrainfo.PersistentTaxiExtraInfo
import com.tudux.taxi.actors.passenger.PersistentTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.PersistentTaxiTripTimeInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.routes.CommonTaxiTripRoutes
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class TaxiTripRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with ScalatestRouteTest
  with SprayJsonSupport with CreateTaxiTripRequestProtocol with CombinedTaxiTripOperationResponseProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip information")
  info("So I should be able to use the resources available to create and delete taxi trip")
  // Create the aggregators
  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator")
    , "cost-aggregator")

  implicit val timeoutRouteTestTimeout = RouteTestTimeout(60.seconds.dilated)
  implicit val timeout: Timeout = Timeout(30.seconds)
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator")
    , "time-aggregator")
  // Create the sharded version of the persistent actors
  val persistentCost: ActorRef = system.actorOf(PersistentTaxiTripCost.props(costAggregatorActor))
  val persistentExtraInfo: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
  val persistentPassenger: ActorRef = system.actorOf(PersistentTaxiTripPassengerInfo.props)
  val persistentTimeInfo: ActorRef = system.actorOf(PersistentTaxiTripTimeInfo.props(timeAggregatorActor))
  //routes
  val routes = CommonTaxiTripRoutes(persistentCost,
    persistentExtraInfo, persistentPassenger, persistentTimeInfo).routes

  Feature("Handle create taxi trip endpoint") {

    Scenario("Create a new taxi trip test case #1") {
      Given("A taxi trip create request")
      val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
        tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
        passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
        storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should send respective messages to the actors and response with an OK status code")
        status shouldBe StatusCodes.Created
      }
    }

    Scenario("Create a new taxi trip test case #2") {
      Given("A taxi trip create request")
      val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 2,
        tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
        passengerCount = 10, tripDistance = 1.53, pickupLongitude = -180, pickupLatitude = -90,
        rateCodeID = 6, storeAndFwdFlag = "N", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
        fareAmount = 9, extra = 1, mtaTax = 0.5, tipAmount = 14.0, tollsAmount = 1,
        improvementSurcharge = 0.3, totalAmount = 2.0)

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should send respective messages to the actors and response with an OK status code")
        status shouldBe StatusCodes.Created
      }
    }


    Scenario("Create a new taxi trip test case #3") {
      Given("A taxi trip create request with empty vendorID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": ,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Create a new taxi trip test case #5") {
      Given("A taxi trip create request with a nor register vendor as vendorID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 3,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")

        status shouldBe StatusCodes.BadRequest
      }
    }


    Scenario("Create a new taxi trip test case #6") {
      Given("A taxi trip create request with a nor register vendor (negative) as vendorID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": -1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")

        status shouldBe StatusCodes.BadRequest
      }
    }


    Scenario("Create a new taxi trip test case #7") {
      Given("A taxi trip create request with empty tpepPickupDatetime")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #8") {
      Given("A taxi trip create request with empty tpepDropoffDatetime")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #9") {
      Given("A taxi trip create request with a invalid pickup date")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-13-33 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }


    Scenario("Create a new taxi trip test case #10") {
      Given("A taxi trip create request with a invalid dropoff date")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-13-33 19:05:42",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }


    Scenario("Create a new taxi trip test case #11") {
      Given("A taxi trip create request with a invalid pickup and dropoff date")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-13-33 19:05:42",
           |  "tpepDropoffDatetime": "2015-13-33 19:05:42",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #12") {
      Given("A taxi trip create request with pickup date older than dropoff")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 20:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1.53,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  Feature("Handle delete taxi trip endpoint") {

    Scenario("Delete a existent taxi trip") {
      Given("A taxi trip create request")
      val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
        tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
        passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
        storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)

      When("a user send a POST request to create a new taxi trip and after that send a DELETE " +
        "request to delete it")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {
        val taxiTripId: String =
          entityAs[CombinedTaxiTripOperationResponse].costResponse.id

        Then("should response with a OK status code")
        Delete(s"/api/yellowtaxi/taxitrip/$taxiTripId") ~> routes ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    Scenario("Delete a with an not existent taxi trip id") {
      When("a user send a DELETE request with a empty taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Delete(s"/api/yellowtaxi/taxitrip/$aNotExistentTaxiTripId") ~> routes ~> check {
        Then("should response with a BadRequest status code")
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

}
