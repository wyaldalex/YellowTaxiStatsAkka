package com.tudux.taxi.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.GivenWhenThen
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.tudux.taxi.http.routes.CommonTaxiTripRoutes
import org.scalatest.matchers.should.Matchers
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit.{TestDuration, TestProbe}
import akka.util.Timeout
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.CreateTaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.CreateTaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.CreateTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.CreateTaxiTripTimeInfo

case class CreateTaxiTripRequest(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String,
  passengerCount: Int, tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
  storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
  paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
  tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)

trait CreateTaxiTripRequestProtocol extends DefaultJsonProtocol {
  implicit val CreateTaxiTripRequestFormat = jsonFormat19(CreateTaxiTripRequest)
}



class TaxiTripRouterSpec extends AnyFeatureSpecLike  with GivenWhenThen with Matchers with ScalatestRouteTest
  with SprayJsonSupport with CreateTaxiTripRequestProtocol{

  info("As a user of the application")
  info("I should be able to handle Taxi Trip information")
  info("So I should be able to use the resources available to create and delete taxi trip")

  override def createActorSystem(): ActorSystem = ActorSystem("taxiTripRouterSpec")

  implicit val timeoutRouteTestTimeout = RouteTestTimeout(60.seconds.dilated)
  implicit val timeout: Timeout = Timeout(30.seconds)

  // Create the sharded version of the persistent actors
  val persistentCostShardRegion: TestProbe = TestProbe()
  val persistentExtraInfoShardedRegion: TestProbe = TestProbe()
  val persistentPassengerShardRegion: TestProbe = TestProbe()
  val persistentTimeInfoShardRegion: TestProbe = TestProbe()

  //routes
  val routes = CommonTaxiTripRoutes(persistentCostShardRegion.ref,
    persistentExtraInfoShardedRegion.ref,
    persistentPassengerShardRegion.ref,
    persistentTimeInfoShardRegion.ref).routes

  Feature("Handle taxi trip endpoints") {

    Scenario("Create a new taxi trip test case #1") {
      Given("A taxi trip create request")
      val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
        tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
        passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
        storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)
      val tripId: String = "TEST-ID-EXAMPLE"
      val operationResponse: OperationResponse = OperationResponse(tripId, Right("Success"))

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should send respective messages to the actors and response with an OK status code")

        persistentCostShardRegion.expectMsgType[CreateTaxiTripCost]
        persistentCostShardRegion.reply(operationResponse)

        persistentExtraInfoShardedRegion.expectMsgType[CreateTaxiTripExtraInfo]
        persistentExtraInfoShardedRegion.reply(operationResponse)


        persistentPassengerShardRegion.expectMsgType[CreateTaxiTripPassengerInfo]
        persistentPassengerShardRegion.reply(operationResponse)


        persistentTimeInfoShardRegion.expectMsgType[CreateTaxiTripTimeInfo]
        persistentTimeInfoShardRegion.reply(operationResponse)

        //status shouldBe StatusCodes.OK
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

      val tripId: String = "TEST-ID-EXAMPLE"
      val operationResponse: OperationResponse = OperationResponse(tripId, Right("Success"))

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should send respective messages to the actors and response with an OK status code")

        persistentCostShardRegion.expectMsgType[CreateTaxiTripCost]
        persistentCostShardRegion.reply(operationResponse)

        persistentExtraInfoShardedRegion.expectMsgType[CreateTaxiTripExtraInfo]
        persistentExtraInfoShardedRegion.reply(operationResponse)


        persistentPassengerShardRegion.expectMsgType[CreateTaxiTripPassengerInfo]
        persistentPassengerShardRegion.reply(operationResponse)


        persistentTimeInfoShardRegion.expectMsgType[CreateTaxiTripTimeInfo]
        persistentTimeInfoShardRegion.reply(operationResponse)

        //status shouldBe StatusCodes.OK
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
}
