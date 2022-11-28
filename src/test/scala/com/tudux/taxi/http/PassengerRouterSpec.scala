package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.fixtures.Routes
import com.tudux.taxi.http.formatters.RouteFormatters.TaxiPassengerInfoProtocol
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class PassengerRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with Routes
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with
  TaxiPassengerInfoProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip Passenger information")
  info("So I should be able to use the resources available to get and update taxi trip passenger")

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

  Feature("Handle get taxi trip passenger endpoint") {

    Scenario("Get time existent taxi trip id") {
      Given("a TaxiTripPassenger to be got it")
      val aTaxiPassengerInfo: TaxiTripPassengerInfo = TaxiTripPassengerInfo(passengerCount = 1)

      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/passenger/$taxiTripId") ~> routes ~> check {

        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripPassengerInfo] shouldBe aTaxiPassengerInfo
      }
    }

    Scenario("Get passenger with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/passenger/$aNotExistentTaxiTripId") ~> routes ~> check {
        Then("should response with a NotFound")
        status shouldBe StatusCodes.NotFound
      }
    }

  }

  Feature("Handle update taxi trip passenger endpoint") {

    Scenario("Update a taxi trip passenger test case #13") {
      Given("A taxi trip passenger update request with empty passengerCount")
      val aUpdateTaxiTripPassengerRequest: String =
        s"""{
           |  "passengerCount": 
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip passenger")
      Put(s"/api/yellowtaxi/passenger/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripPassengerRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip passenger test case #14") {
      Given("A taxi trip passenger update request with something different to a number as passengerCount")
      val aUpdateTaxiTripPassengerRequest: String =
        s"""{
           |  "passengerCount": A
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip passenger")
      Put(s"/api/yellowtaxi/passenger/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripPassengerRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip passenger test case #15") {
      Given("A taxi trip passenger update request with zero as passengerCount")
      val aUpdateTaxiTripPassengerRequest: String =
        s"""{
           |  "passengerCount": 0
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip passenger")
      Put(s"/api/yellowtaxi/passenger/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripPassengerRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip passenger test case #16") {
      Given("A taxi trip passenger update request with a negative number as passengerCount")
      val aUpdateTaxiTripPassengerRequest: String =
        s"""{
           |  "passengerCount": -1
           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip passenger")
      Put(s"/api/yellowtaxi/passenger/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripPassengerRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }


  }


}
