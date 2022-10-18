package com.tudux.taxi.actors.passenger

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiTripPassengerInfo(passengerCount: Int, deletedFlag: Boolean = false)

sealed trait TaxiTripPassengerInfoCommand
object TaxiTripPassengerInfoCommand {
  case class CreateTaxiTripPassengerInfo(tripId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfo) extends TaxiTripPassengerInfoCommand
  case class GetTaxiTripPassengerInfo(tripId: String) extends TaxiTripPassengerInfoCommand
  case class UpdateTaxiTripPassenger(tripId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfo) extends TaxiTripPassengerInfoCommand
  case class DeleteTaxiTripPassenger(tripId: String) extends TaxiTripPassengerInfoCommand
  case object GetTotalPassengerInfoLoaded
}


sealed trait TaxiTripPassengerInfoEvent
object TaxiTripPassengerInfoStatEvent{
  case class TaxiTripPassengerInfoCreatedEvent(tripId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfo) extends TaxiTripPassengerInfoEvent
  case class UpdatedTaxiTripPassengerEvent(tripId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfo) extends TaxiTripPassengerInfoEvent
  case class DeletedTaxiTripPassengerEvent(tripId: String) extends TaxiTripPassengerInfoEvent
}

object PersistentTaxiTripPassengerInfo {
  def props(id: String): Props = Props(new PersistentTaxiTripPassengerInfo(id))
}
class PersistentTaxiTripPassengerInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripPassengerInfoCommand._
  import TaxiTripPassengerInfoStatEvent._

  var state : TaxiTripPassengerInfo = TaxiTripPassengerInfo(0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripPassengerInfo(tripId,taxiTripPassengerInfoStat) =>
      persist(TaxiTripPassengerInfoCreatedEvent(tripId,taxiTripPassengerInfoStat)) { _ =>
        log.info(s"Creating Passenger Info Stat $taxiTripPassengerInfoStat")
        state = taxiTripPassengerInfoStat
      }
    case GetTaxiTripPassengerInfo(tripId) =>
      sender() ! state
    case UpdateTaxiTripPassenger(tripId,taxiTripPassengerInfoStat) =>
      log.info(s"Applying update for Passenger Info for id $tripId")
      persist(UpdatedTaxiTripPassengerEvent(tripId, taxiTripPassengerInfoStat)) { _ =>
        state = taxiTripPassengerInfoStat
      }
    case DeleteTaxiTripPassenger(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripPassengerEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripPassengerInfoCreatedEvent(tripId,taxiTripPassengerInfoStat) =>
      log.info(s"Recovering Passenger Info Stat $tripId")
      state = taxiTripPassengerInfoStat
    case   UpdatedTaxiTripPassengerEvent(tripId,taxiTripPassengerInfoStat) =>
      log.info(s"Recovered Update Event applied for Passenger info Id: $tripId")
      state = taxiTripPassengerInfoStat
    case DeletedTaxiTripPassengerEvent(tripId) =>
      log.info(s"Recovered Deleted Event applied for Passenger info Id: $tripId")
      state = state.copy(deletedFlag = true)
  }
}


