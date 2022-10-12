package com.tudux.taxi.actors.passenger

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiTripPassengerInfoStat(passengerCount: Int,deletedFlag: Boolean = false)

sealed trait TaxiTripPassengerInfoCommand
object TaxiTripPassengerInfoStatCommand {
  case class CreateTaxiTripPassengerInfoStat(statId: String,taxiTripPassengerInfoStat: TaxiTripPassengerInfoStat) extends TaxiTripPassengerInfoCommand
  case class GetTaxiPassengerInfoStat(statId: String) extends TaxiTripPassengerInfoCommand
  case class UpdateTaxiPassenger(statId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfoStat) extends TaxiTripPassengerInfoCommand
  case class DeleteTaxiTripPassenger(statId: String) extends TaxiTripPassengerInfoCommand
  case object GetTotalPassengerInfoLoaded
}


sealed trait TaxiTripPassengerInfoEvent
object TaxiTripPassengerInfoStatEvent{
  case class TaxiTripPassengerInfoStatCreatedEvent(statId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfoStat) extends TaxiTripPassengerInfoEvent
  case class UpdatedTaxiPassengerEvent(statId: String, taxiTripPassengerInfoStat: TaxiTripPassengerInfoStat) extends TaxiTripPassengerInfoEvent
  case class DeletedTaxiTripPassengerEvent(statId: String) extends TaxiTripPassengerInfoEvent
}

object PersistentTaxiTripPassengerInfo {
  def props(id: String): Props = Props(new PersistentTaxiTripPassengerInfo(id))
}
class PersistentTaxiTripPassengerInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripPassengerInfoStatCommand._
  import TaxiTripPassengerInfoStatEvent._

  var state : TaxiTripPassengerInfoStat = TaxiTripPassengerInfoStat(0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripPassengerInfoStat(statId,taxiTripPassengerInfoStat) =>
      persist(TaxiTripPassengerInfoStatCreatedEvent(statId,taxiTripPassengerInfoStat)) { _ =>
        log.info(s"Creating Passenger Info Stat $taxiTripPassengerInfoStat")
        state = taxiTripPassengerInfoStat
      }
    case GetTaxiPassengerInfoStat(statId) =>
      sender() ! state
    case UpdateTaxiPassenger(statId,taxiTripPassengerInfoStat) =>
      log.info(s"Applying update for Passenger Info for id $statId")
      persist(UpdatedTaxiPassengerEvent(statId, taxiTripPassengerInfoStat)) { _ =>
        state = taxiTripPassengerInfoStat
      }
    case DeleteTaxiTripPassenger(statId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripPassengerEvent(statId)) { _ =>
        state = state.copy(deletedFlag = true)
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripPassengerInfoStatCreatedEvent(statId,taxiTripPassengerInfoStat) =>
      log.info(s"Recovering Passenger Info Stat $taxiTripPassengerInfoStat")
      state = taxiTripPassengerInfoStat
    case   UpdatedTaxiPassengerEvent(statId,taxiTripPassengerInfoStat) =>
      log.info(s"Recovered Update Event applied for Passenger info Id: $statId")
      state = taxiTripPassengerInfoStat
    case DeletedTaxiTripPassengerEvent(statId) =>
      log.info(s"Recovered Deleted Event applied for Passenger info Id: $statId")
      state = state.copy(deletedFlag = true)
  }
}


