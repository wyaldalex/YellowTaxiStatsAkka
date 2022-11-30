package com.tudux.taxi.actors.passenger

import akka.actor.{ActorSystem, Kill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class PersistentTaxiTripPassengerInfoSpec extends TestKit(ActorSystem("PersistentTaxiExtraInfoSpec"))
  with AnyFeatureSpecLike with GivenWhenThen with ImplicitSender with Matchers {

  info("As a external actor")
  info("I should be able to handle all the basic operations")
  info("Consequently the response and state should correspond")

  Feature("handle recover") {
    Scenario("Recover state after a restart happens") {
      Given("a PersistentTaxiTripPassengerInfo actor and commands")
      // Creating Actor
      val APersistentTaxiTripPassengerInfo = system.actorOf(Props(new PersistentTaxiTripPassengerInfo {
        override def persistenceId: String = "persistent-taxi-extra-info-test-id"
      }))

      // Auxiliary variables for commands
      val tripId: String = "SADFASDF2886J865DASJKANDS45676IWQEWFLWF234LQHJW"
      val createInfo: TaxiTripPassengerInfo = TaxiTripPassengerInfo(1)
      val updateInfo: TaxiTripPassengerInfo = TaxiTripPassengerInfo(2)

      // Create command
      val createCommand = TaxiTripPassengerInfoCommand.CreateTaxiTripPassengerInfo(tripId, createInfo)
      // Update command
      val updateCommand = TaxiTripPassengerInfoCommand.UpdateTaxiTripPassenger(tripId, updateInfo)
      // Delete command
      val deleteCommand = TaxiTripPassengerInfoCommand.DeleteTaxiTripPassenger(tripId)

      // Get command
      val getCommand = TaxiTripPassengerInfoCommand.GetTaxiTripPassengerInfo(tripId)

      // Sending create command
      APersistentTaxiTripPassengerInfo ! createCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending update command
      APersistentTaxiTripPassengerInfo ! updateCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      // Sending delete command
      APersistentTaxiTripPassengerInfo ! deleteCommand
      expectMsg(OperationResponse(tripId, Right("Success")))

      When("A restart happens")
      APersistentTaxiTripPassengerInfo ! Kill

      Then("APersistentTaxiTripPassengerInfo should restore his state")
      val ARestartedPersistentTaxiTripPassengerInfo = system.actorOf(Props(new
          PersistentTaxiTripPassengerInfo {
        override def persistenceId: String = "persistent-taxi-extra-info-test-id"
      }))

      ARestartedPersistentTaxiTripPassengerInfo ! getCommand
      expectMsg(updateInfo.copy(deletedFlag = true))
    }
  }

}
