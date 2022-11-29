package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class PersistentTaxiExtraInfoSpec
  extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {

  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle basic operations") {
    Scenario("External actor send a create command") {
      Given("a TaxiTripExtraInfo actor and create command")
      val APersistentTaxiExtraInfoActor: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val taxiExtraInfoStat: TaxiTripExtraInfo = TaxiTripExtraInfo(pickupLongitude = -73.991127014160156,
        pickupLatitude = 40.750080108642578, rateCodeID = 1, storeAndFwdFlag = "N",
        dropoffLongitude = -73.988609313964844, dropoffLatitude = 40.734889984130859)
      val createCommand = TaxiTripExtraInfoCommand.CreateTaxiTripExtraInfo(tripId, taxiExtraInfoStat)

      When("receive create command")
      APersistentTaxiExtraInfoActor ! createCommand

      Then("response and state should correspond")
      expectMsg(OperationResponse(tripId,Right("Success")))
    }
  }

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentTaxiExtraInfo actor and commands")
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val APersistentTaxiExtraInfoActor = system.actorOf(PersistentTaxiExtraInfo.props)

      val taxiExtraInfoStat: TaxiTripExtraInfo = TaxiTripExtraInfo(pickupLongitude = -73.991127014160156,
        pickupLatitude = 40.750080108642578, rateCodeID = 1, storeAndFwdFlag = "N", dropoffLongitude =
          -73.988609313964844, dropoffLatitude = 40.734889984130859)
      val createCommand = TaxiTripExtraInfoCommand.CreateTaxiTripExtraInfo(tripId, taxiExtraInfoStat)
      val getCommand = TaxiTripExtraInfoCommand.GetTaxiTripExtraInfo(tripId)

      APersistentTaxiExtraInfoActor ! createCommand
      expectMsg(OperationResponse(tripId,Right("Success")))
      //APersistentTaxiExtraInfoActor ! getCommand
      //expectMsg(taxiExtraInfoStat)

      When("A restart happens")
      APersistentTaxiExtraInfoActor ! Kill

      Then("PersistentTaxiExtraInfo should restore his state")
      val ARestartedPersistentTaxiExtraInfoActor = system.actorOf(PersistentTaxiExtraInfo.props)

      ARestartedPersistentTaxiExtraInfoActor ! getCommand
      expectMsg(taxiExtraInfoStat)
    }
  }

}
