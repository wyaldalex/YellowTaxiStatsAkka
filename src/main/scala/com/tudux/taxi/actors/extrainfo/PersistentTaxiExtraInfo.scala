package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiTripExtraInfo(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                             storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double, deletedFlag: Boolean = false)

sealed trait TaxiExtraInfoCommand
object TaxiTripExtraInfoCommand {
  case class CreateTaxiTripExtraInfo(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoCommand
  case class GetTaxiTripExtraInfo(tripId: String) extends TaxiExtraInfoCommand
  case class UpdateTaxiTripExtraInfo(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoCommand
  case class DeleteTaxiTripExtraInfo(tripId: String) extends TaxiExtraInfoCommand
  case object GetTotalExtraInfoLoaded
}


sealed trait TaxiExtraInfoEvent
object TaxiExtraInfoStatEvent{
  case class TaxiTripExtraInfoCreatedEvent(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoEvent
  case class TaxiTripExtraInfoUpdatedEvent(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoEvent
  case class DeletedTaxiTripExtraInfoEvent(tripId: String) extends TaxiExtraInfoEvent
}

object PersistentTaxiExtraInfo {
  def props(id: String): Props = Props(new PersistentTaxiExtraInfo(id))
}
class PersistentTaxiExtraInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripExtraInfoCommand._
  import TaxiExtraInfoStatEvent._

  //Persistent Actor State
  //var statExtraInfoMap : Map[String,TaxiExtraInfoStat] = Map.empty
  var state : TaxiTripExtraInfo = TaxiTripExtraInfo(0,0,0,"",0,0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripExtraInfo(tripId,taxiExtraInfoStat) =>
      //throw new RuntimeException("Mock Actor Failure") //Simulate Actor failure
      persist(TaxiTripExtraInfoCreatedEvent(tripId,taxiExtraInfoStat)) { _ =>
        log.info(s"Creating Extra Info Stat $taxiExtraInfoStat")
        state = taxiExtraInfoStat
      }
    case UpdateTaxiTripExtraInfo(tripId,taxiExtraInfoStat) =>
      log.info(s"Updating Extra Info Stat $taxiExtraInfoStat")
      persist(TaxiTripExtraInfoUpdatedEvent(tripId, taxiExtraInfoStat)) { _ =>
        state = taxiExtraInfoStat
      }

    case GetTaxiTripExtraInfo(tripId) =>
      sender() ! state
    case DeleteTaxiTripExtraInfo(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripExtraInfoEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
      }
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripExtraInfoCreatedEvent(tripId,taxiExtraInfoStat) =>
      log.info(s"Recovering Extra Info Stat $tripId")
      state = taxiExtraInfoStat
    case TaxiTripExtraInfoUpdatedEvent(tripId,taxiExtraInfoStat) =>
      log.info(s"Recovering Updated Extra Info Stat $tripId")
      state = taxiExtraInfoStat
    case DeletedTaxiTripExtraInfoEvent(tripId) =>
      log.info(s"Recovering Deleted Extra Info Stat for $tripId")
      state = state.copy(deletedFlag = true)

  }
}


