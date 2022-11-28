package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.fixtures.Routes
import com.tudux.taxi.http.formatters.RouteFormatters.TaxiTimeInfoStatProtocol
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class TimeRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with Routes
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with
  TaxiTimeInfoStatProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip Cost information")
  info("So I should be able to use the resources available to get and update taxi trip cost")

  // initializing variables
  var taxiTripId: String = ""

  override def beforeEach() {
    val aCreateTaxiTripRequest: CreateTaxiTripRequest = CreateTaxiTripRequest(vendorID = 1,
      tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
      passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
      storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
      fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
      totalAmount = 2.0)

    Post("/api/yellowtaxi/taxitrip", aCreateTaxiTripRequest) ~> routes ~> check {
      taxiTripId = entityAs[CombinedTaxiTripOperationResponse].costResponse.id
    }
    super.beforeEach()
  }

  override def afterEach() {
    try super.afterEach()
    finally Delete(s"/api/yellowtaxi/taxitrip/$taxiTripId") ~> routes
  }

  Feature("Handle get taxi trip time endpoint") {

    Scenario("Get time existent taxi trip id") {
      Given("a TaxiTripCost to be got it")
      val aTaxiTripTimeInfo: TaxiTripTimeInfo = TaxiTripTimeInfo(tpepPickupDatetime = "2015-01-15 19:05:42",
        tpepDropoffDatetime = "2015-01-15 19:16:18")

      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/time/$taxiTripId") ~> routes ~> check {


        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripTimeInfo] shouldBe aTaxiTripTimeInfo
      }
    }

    Scenario("Get cost with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/time/$aNotExistentTaxiTripId") ~> routes ~> check {
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
      Put(s"/api/yellowtaxi/time/$taxiTripId", aTaxiTripTimeInfo) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
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
        aUpdateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

}
