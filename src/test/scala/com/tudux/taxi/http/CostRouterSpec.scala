package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.http.HttpTestUtility._
import com.tudux.taxi.http.fixtures.Routes
import com.tudux.taxi.http.formatters.RouteFormatters.TaxiCostStatProtocol
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class CostRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with Routes
  with BeforeAndAfterEach with SprayJsonSupport with CreateTaxiTripRequestProtocol
  with CombinedTaxiTripOperationResponseProtocol with OperationResponseProtocol with TaxiCostStatProtocol {

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

  Feature("Handle get taxi trip cost endpoint") {

    Scenario("Get cost existent taxi trip id") {
      Given("a TaxiTripCost to be got it")
      val aTaxiTripCost: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)

      When("a user send a GET request to get the specify taxi trip cost")
      Get(s"/api/yellowtaxi/cost/$taxiTripId") ~> routes ~> check {
        Then("should response with a OK status code AND id should be equal to taxiTripId")
        status shouldBe StatusCodes.OK
        entityAs[TaxiTripCost] shouldBe aTaxiTripCost
      }
    }

    Scenario("Get cost with an not existent taxi trip id") {
      When("a user send a Get request with a not existent taxiTripId")
      val aNotExistentTaxiTripId: String = "ThisIdDontExistOnTheSystem"
      Get(s"/api/yellowtaxi/cost/$aNotExistentTaxiTripId") ~> routes ~> check {
        Then("should response with a NotFound")
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  Feature("Handle update taxi trip cost endpoint") {
    Scenario("Update a taxi trip test case #1") {
      Given("A taxi trip cost request")
      val aTaxiTripCost: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)

      When("a user send a PUT request to update a taxi trip")
      Put(s"/api/yellowtaxi/cost/$taxiTripId", aTaxiTripCost) ~> routes ~> check {

        Then("should response with an OK status code")
        status shouldBe StatusCodes.OK
        entityAs[OperationResponse].id shouldBe taxiTripId
      }
    }

    Scenario("Update a  taxi trip cost test case #2") {
      Given("A taxi trip cost request")
      val aTaxiTripCost: TaxiTripCost = TaxiTripCost(vendorID = 1, tripDistance = 1.53, paymentType = 2,
        fareAmount = 9, extra = 1, mtaTax = 0.5, tipAmount = 14.0, tollsAmount = 1,
        improvementSurcharge = 0.3, totalAmount = 2.0)

      When("a user send a Put request to update a taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId", aTaxiTripCost) ~> routes ~> check {

        Then("should response with an OK status code")
        status shouldBe StatusCodes.OK
        entityAs[OperationResponse].id shouldBe taxiTripId
      }
    }

    Scenario("Update a  taxi trip cost test case #3") {
      Given("A taxi trip cost update request with empty vendorID")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": ,

           |  "tripDistance": 1.53,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #5") {
      Given("A taxi trip cost update request with a not register vendor as vendorID")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 3,

           |  "tripDistance": 1.53,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")

        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #6") {
      Given("A taxi trip cost update request with a not register vendor (negative) as vendorID")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": -1,

           |  "tripDistance": 1.53,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {
        Then("should respond with a bad request status code")

        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #17") {
      Given("A taxi trip cost update request with empty tripDistance")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": ,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #18") {
      Given("A taxi trip cost update request with empty tripDistance")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": "A",

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #19") {
      Given("A taxi trip cost update request with zero as tripDistance")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 0,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #20") {
      Given("A taxi trip cost update request with negative number as tripDistance")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": -1,

           |  "paymentType": 2,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #44") {
      Given("A taxi trip cost update request with empty as paymentType")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": ,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #45") {
      Given("A taxi trip cost update request with something different to a number as paymentType")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": "A",
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #46") {
      Given("A taxi trip cost update request with a negative number as paymentType")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": -1,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #47") {
      Given("A taxi trip cost update request with a limit zero as paymentType")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 0,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #48") {
      Given("A taxi trip cost update request with a limit 7 as paymentType")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 7,
           |  "fareAmount": 9,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #49") {
      Given("A taxi trip cost update request with empty as fareAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": "",
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #50") {
      Given("A taxi trip cost update request with empty as fareAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": "A",
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #51") {
      Given("A taxi trip cost update request with empty as fareAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 0,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #52") {
      Given("A taxi trip cost update request with empty as fareAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": -1,
           |  "extra": 0,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #53") {
      Given("A taxi trip cost update request with empty as extra")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": ,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #54") {
      Given("A taxi trip cost update request with something different to a number as extra")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": "A",
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #55") {
      Given("A taxi trip cost update request with a negative number as extra")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": -1,
           |  "mtaTax": 0,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #56") {
      Given("A taxi trip cost update request with empty as mtaTax")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": ,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #57") {
      Given("A taxi trip cost update request with something different to a number as mtaTax")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": "A",
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #58") {
      Given("A taxi trip cost update request with a negative number as mtaTax")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": -1,
           |  "tipAmount": 0,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #59") {
      Given("A taxi trip cost update request with empty as tipAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": ,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #60") {
      Given("A taxi trip cost update request with something different to a number as tipAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": A,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #61") {
      Given("A taxi trip cost update request with a negative number as tipAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": -1,
           |  "tollsAmount": 0,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #62") {
      Given("A taxi trip cost update request with empty as tollsAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": ,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #63") {
      Given("A taxi trip cost update request with something different to a number as tollsAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": "A",
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #64") {
      Given("A taxi trip cost update request with a negative number as tollsAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": -1,
           |  "improvementSurcharge": 0,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #65") {
      Given("A taxi trip cost update request with empty as improvementSurcharge")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": ,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #66") {
      Given("A taxi trip cost update request with a negative number as tollsAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": -1,
           |  "totalAmount": 2.0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #67") {
      Given("A taxi trip cost update request with empty as totalAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount":
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #68") {
      Given("A taxi trip cost update request with something different to a number as totalAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": "A"
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should reject the request")
        rejections should not be empty
      }
    }

    Scenario("Update a  taxi trip cost test case #69") {
      Given("A taxi trip cost update request with zero as totalAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": 0
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

    Scenario("Update a  taxi trip cost test case #70") {
      Given("A taxi trip cost update request with a negative number as totalAmount")
      val aUpdateTaxiTripRequest: String =
        s"""{
           |  "vendorID": 1,

           |  "tripDistance": 1,

           |  "paymentType": 1,
           |  "fareAmount": 1,
           |  "extra": 1,
           |  "mtaTax": 1,
           |  "tipAmount": 1,
           |  "tollsAmount": 1,
           |  "improvementSurcharge": 1,
           |  "totalAmount": -1
           |} """.stripMargin

      When("a user send a Put request to Update a  taxi trip cost")
      Put(s"/api/yellowtaxi/cost/$taxiTripId").withEntity(ContentTypes.`application/json`,
        aUpdateTaxiTripRequest) ~> routes ~> check {

        Then("should response with bad request status code")
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

}
