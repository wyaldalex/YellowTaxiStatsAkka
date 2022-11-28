package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.fixtures.Routes
import com.tudux.taxi.http.formatters.RouteFormatters.TaxiExtraInfoProtocol
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class ExtraInfoRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with Routes
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with TaxiExtraInfoProtocol {

  info("As a user of the application")
  info("I should be able to handle Taxi Trip ExtraInfo information")
  info("So I should be able to use the resources available to get and update taxi trip ExtraInfo")

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

  Feature("Handle get taxi trip extraInfo endpoint") {

    Scenario("Get extraInfo existent taxi trip id") {
      Given("a TaxiTripCost to be got it")
      val aTaxiTripExtraInfo: TaxiTripExtraInfo = TaxiTripExtraInfo(pickupLongitude = 180,
        pickupLatitude = 90, rateCodeID = 1, storeAndFwdFlag = "Y", dropoffLongitude = 180,
        dropoffLatitude = 90)

      When("a user send a GET request to get the specify taxi trip extraInfo")
      Get(s"/api/yellowtaxi/extrainfo/$taxiTripId") ~> routes ~> check {
        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripExtraInfo] shouldBe aTaxiTripExtraInfo
      }
    }

    Scenario("Get extraInfo with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/extrainfo/$aNotExistentTaxiTripId") ~> routes ~> check {
        Then("should response with a NotFound")
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  Feature("Handle update taxi trip extraInfo endpoint") {
    Scenario("Update  a taxi trip extraInfo test case #1") {
      Given("A taxi trip update request")
      val aTaxiTripExtraInfo: TaxiTripExtraInfo = TaxiTripExtraInfo(pickupLongitude = 180,
        pickupLatitude = 90, rateCodeID = 1, storeAndFwdFlag = "Y", dropoffLongitude = 180,
        dropoffLatitude = 90)

      When("a user send a Put request to Update  a taxi trip extraInfo")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId", aTaxiTripExtraInfo) ~> routes ~> check {
        Then("should response with an Created status code")
        status shouldBe StatusCodes.Created
      }
    }

    Scenario("Update  a taxi trip extraInfo test case #2") {
      Given("A taxi trip update request")
      val aTaxiTripExtraInfo: TaxiTripExtraInfo = TaxiTripExtraInfo(pickupLongitude = -180,
        pickupLatitude = -90, rateCodeID = 6, storeAndFwdFlag = "N", dropoffLongitude = 180,
        dropoffLatitude = 90)

      When("a user send a Put request to Update a taxi trip extraInfo")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId", aTaxiTripExtraInfo) ~> routes ~> check {

        Then("should response with an Created status code")
        status shouldBe StatusCodes.Created
      }
    }

    Scenario("Update a taxi trip test case #21") {
      Given("A taxi trip update request with empty pickupLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #22") {
      Given("A taxi trip update request with something different to a number as pickupLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "A",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #23") {
      Given("A taxi trip update request with limit 181 as pickupLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "181",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #24") {
      Given("A taxi trip update request with limit -181 as pickupLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "-181",
           |  "pickupLatitude": "90",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #25") {
      Given("A taxi trip update request with empty pickupLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #26") {
      Given("A taxi trip update request with something different to a number as pickupLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "A",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #27") {
      Given("A taxi trip update request with limit 91 as pickupLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "91",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #28") {
      Given("A taxi trip update request with limit -91 as pickupLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "-180",
           |  "pickupLatitude": "-91",
           |  "rateCodeID": 1,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #29") {
      Given("A taxi trip update request with empty rateCodeID")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": ,
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #30") {
      Given("A taxi trip update request with something different to a number as rateCodeID")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "A",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #31") {
      Given("A taxi trip update request with a negative number as rateCodeID")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "-1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #32") {
      Given("A taxi trip update request with a zero as rateCodeID")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "0",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #33") {
      Given("A taxi trip update request with 7 as rateCodeID")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "7",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #34") {
      Given("A taxi trip update request with empty as storeAndFwdFlag")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": ,
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #35") {
      Given("A taxi trip update request with something different to a Y and N as storeAndFwdFlag")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "A",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #36") {
      Given("A taxi trip update request with empty as dropoffLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #37") {
      Given("A taxi trip update request with empty as dropoffLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "A",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #38") {
      Given("A taxi trip update request with limit 181 as dropoffLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "181",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #39") {
      Given("A taxi trip update request with limit -181 as dropoffLongitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "-181",
           |  "dropoffLatitude": "90"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #40") {
      Given("A taxi trip update request with empty as dropoffLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": ""

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #41") {
      Given("A taxi trip update request with empty as dropoffLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "A"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a taxi trip test case #42") {
      Given("A taxi trip update request with limit 91 as dropoffLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "91"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a taxi trip test case #43") {
      Given("A taxi trip update request with limit -91 as dropoffLatitude")
      val aUpdateTaxiTripExtraInfoRequest: String =
        s"""{

           |  "pickupLongitude": "180",
           |  "pickupLatitude": "90",
           |  "rateCodeID": "1",
           |  "storeAndFwdFlag": "Y",
           |  "dropoffLongitude": "180",
           |  "dropoffLatitude": "-91"

           |} """.stripMargin

      When("a user send a Put request to Update a taxi trip")
      Put(s"/api/yellowtaxi/extrainfo/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripExtraInfoRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

}
