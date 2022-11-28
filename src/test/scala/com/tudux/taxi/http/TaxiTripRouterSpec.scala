package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.fixtures.Routes
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class TaxiTripRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with Routes
  with SprayJsonSupport with CreateTaxiTripRequestProtocol with CombinedTaxiTripOperationResponseProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip information")
  info("So I should be able to use the resources available to create and delete taxi trip")

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

        Then("should response with an Created status code")
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

        Then("should response with an Created status code")
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

    Scenario("Create a new taxi trip test case #4") {
      Given("A taxi trip create request with something different to a number vendorID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": A,
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
      Given("A taxi trip create request with a not register vendor as vendorID")
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
      Given("A taxi trip create request with a not register vendor (negative) as vendorID")
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

    Scenario("Create a new taxi trip test case #13") {
      Given("A taxi trip create request with empty passengerCount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": ,
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

    Scenario("Create a new taxi trip test case #14") {
      Given("A taxi trip create request with something different to a number as passengerCount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": A,
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

    Scenario("Create a new taxi trip test case #15") {
      Given("A taxi trip create request with zero as passengerCount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 0,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #16") {
      Given("A taxi trip create request with a negative number as passengerCount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": -1,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #17") {
      Given("A taxi trip create request with empty tripDistance")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": ,
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

    Scenario("Create a new taxi trip test case #18") {
      Given("A taxi trip create request with empty tripDistance")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": "A",
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

    Scenario("Create a new taxi trip test case #19") {
      Given("A taxi trip create request with zero as tripDistance")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 0,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #20") {
      Given("A taxi trip create request with negative number as tripDistance")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": -1,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #21") {
      Given("A taxi trip create request with empty pickupLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "",
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

    Scenario("Create a new taxi trip test case #22") {
      Given("A taxi trip create request with something different to a number as pickupLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "A",
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

    Scenario("Create a new taxi trip test case #23") {
      Given("A taxi trip create request with limit 181 as pickupLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "181",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #24") {
      Given("A taxi trip create request with limit -181 as pickupLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "-181",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #25") {
      Given("A taxi trip create request with empty pickupLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "",
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

    Scenario("Create a new taxi trip test case #26") {
      Given("A taxi trip create request with something different to a number as pickupLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "A",
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

    Scenario("Create a new taxi trip test case #27") {
      Given("A taxi trip create request with limit 91 as pickupLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "91",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #28") {
      Given("A taxi trip create request with limit -91 as pickupLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "-180",
           |  "pickupLatitude": "-91",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #29") {
      Given("A taxi trip create request with empty rateCodeID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": ,
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

    Scenario("Create a new taxi trip test case #30") {
      Given("A taxi trip create request with something different to a number as rateCodeID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "A",
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

    Scenario("Create a new taxi trip test case #31") {
      Given("A taxi trip create request with a negative number as rateCodeID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "-1",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #32") {
      Given("A taxi trip create request with a zero as rateCodeID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "0",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #33") {
      Given("A taxi trip create request with 7 as rateCodeID")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "7",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #34") {
      Given("A taxi trip create request with empty as storeAndFwdFlag")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": ,
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

    Scenario("Create a new taxi trip test case #35") {
      Given("A taxi trip create request with something different to a Y and N as storeAndFwdFlag")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "A",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #36") {
      Given("A taxi trip create request with empty as dropoffLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "",
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

    Scenario("Create a new taxi trip test case #37") {
      Given("A taxi trip create request with empty as dropoffLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "A",
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

    Scenario("Create a new taxi trip test case #38") {
      Given("A taxi trip create request with limit 181 as dropoffLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "181",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #39") {
      Given("A taxi trip create request with limit -181 as dropoffLongitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "-181",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #40") {
      Given("A taxi trip create request with empty as dropoffLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "",
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

    Scenario("Create a new taxi trip test case #41") {
      Given("A taxi trip create request with empty as dropoffLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "A",
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

    Scenario("Create a new taxi trip test case #42") {
      Given("A taxi trip create request with limit 91 as dropoffLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "91",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #43") {
      Given("A taxi trip create request with limit -91 as dropoffLatitude")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "-91",
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #44") {
      Given("A taxi trip create request with empty as paymentType")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": ,
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

    Scenario("Create a new taxi trip test case #45") {
      Given("A taxi trip create request with something different to a number as paymentType")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": "A",
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

    Scenario("Create a new taxi trip test case #46") {
      Given("A taxi trip create request with a negative number as paymentType")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": -1,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #47") {
      Given("A taxi trip create request with a limit zero as paymentType")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 0,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #48") {
      Given("A taxi trip create request with a limit 7 as paymentType")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 7,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #49") {
      Given("A taxi trip create request with empty as fareAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": "",
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

    Scenario("Create a new taxi trip test case #50") {
      Given("A taxi trip create request with empty as fareAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": "A",
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

    Scenario("Create a new taxi trip test case #51") {
      Given("A taxi trip create request with empty as fareAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 0,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #52") {
      Given("A taxi trip create request with empty as fareAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": -1,
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

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #53") {
      Given("A taxi trip create request with empty as extra")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": ,
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

    Scenario("Create a new taxi trip test case #54") {
      Given("A taxi trip create request with something different to a number as extra")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": "A",
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

    Scenario("Create a new taxi trip test case #55") {
      Given("A taxi trip create request with a negative number as extra")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": -1,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #56") {
      Given("A taxi trip create request with empty as mtaTax")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": ,
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

    Scenario("Create a new taxi trip test case #57") {
      Given("A taxi trip create request with something different to a number as mtaTax")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": "A",
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

    Scenario("Create a new taxi trip test case #58") {
      Given("A taxi trip create request with a negative number as mtaTax")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": -1,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #59") {
      Given("A taxi trip create request with empty as tipAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": ,
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

    Scenario("Create a new taxi trip test case #60") {
      Given("A taxi trip create request with something different to a number as tipAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": A,
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

    Scenario("Create a new taxi trip test case #61") {
      Given("A taxi trip create request with a negative number as tipAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": -1,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #62") {
      Given("A taxi trip create request with empty as tollsAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": ,
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

    Scenario("Create a new taxi trip test case #63") {
      Given("A taxi trip create request with something different to a number as tollsAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": "A",
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

    Scenario("Create a new taxi trip test case #64") {
      Given("A taxi trip create request with a negative number as tollsAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": -1,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #65") {
      Given("A taxi trip create request with empty as improvementSurcharge")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": ,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Create a new taxi trip test case #66") {
      Given("A taxi trip create request with a negative number as tollsAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": -1,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #67") {
      Given("A taxi trip create request with empty as totalAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount":
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Create a new taxi trip test case #68") {
      Given("A taxi trip create request with something different to a number as totalAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": "A"
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Create a new taxi trip test case #69") {
      Given("A taxi trip create request with zero as totalAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": 0
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Create a new taxi trip test case #70") {
      Given("A taxi trip create request with a negative number as totalAmount")
      val aCreateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,
           |  "tpepPickupDatetime": "2015-01-15 19:05:42",
           |  "tpepDropoffDatetime": "2015-01-15 19:16:18",
           |  "passengerCount": 1,
           |  "tripDistance": 1,
           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90",
           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": -1
           |} """.stripMargin

      When("a user send a POST request to create a new taxi trip")
      Post("/api/yellowtaxi/taxitrip").withEntity(ContentTypes.`application/json`,
        aCreateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
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
      When("a user send a DELETE request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Delete(s"/api/yellowtaxi/taxitrip/$aNotExistentTaxiTripId") ~> routes ~> check {
        Then("should response with a BadRequest status code")
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

}
