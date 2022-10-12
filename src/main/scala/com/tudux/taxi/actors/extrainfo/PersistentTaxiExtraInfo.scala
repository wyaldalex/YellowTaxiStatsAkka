package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiExtraInfoStat(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                             storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double, deletedFlag: Boolean = false)

sealed trait TaxiExtraInfoCommand
object TaxiExtraInfoStatCommand {
  case class CreateTaxiExtraInfoStat(statId: String,taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoCommand
  case class GetTaxiExtraInfoStat(statId: String) extends TaxiExtraInfoCommand
  case class UpdateTaxiExtraInfoStat(statId: String,taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoCommand
  case class DeleteTaxiExtraInfo(statId: String) extends TaxiExtraInfoCommand
  case object GetTotalExtraInfoLoaded
}


sealed trait TaxiExtraInfoEvent
object TaxiExtraInfoStatEvent{
  case class TaxiExtraInfoStatCreatedEvent(statId: String, taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoEvent
  case class TaxiExtraInfoStatUpdatedEvent(statId: String, taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoEvent
  case class DeletedTaxiExtraInfoEvent(statId: String) extends TaxiExtraInfoEvent
}

object PersistentTaxiExtraInfo {
  def props(id: String): Props = Props(new PersistentTaxiExtraInfo(id))
}
class PersistentTaxiExtraInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiExtraInfoStatCommand._
  import TaxiExtraInfoStatEvent._

  //Persistent Actor State
  //var statExtraInfoMap : Map[String,TaxiExtraInfoStat] = Map.empty
  var state : TaxiExtraInfoStat = TaxiExtraInfoStat(0,0,0,"",0,0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiExtraInfoStat(statId,taxiExtraInfoStat) =>
      //throw new RuntimeException("Mock Actor Failure") //Simulate Actor failure
      persist(TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat)) { _ =>
        log.info(s"Creating Extra Info Stat $taxiExtraInfoStat")
        state = taxiExtraInfoStat
      }
    case UpdateTaxiExtraInfoStat(statId,taxiExtraInfoStat) =>
      log.info(s"Updating Extra Info Stat $taxiExtraInfoStat")
      persist(TaxiExtraInfoStatUpdatedEvent(statId, taxiExtraInfoStat)) { _ =>
        state = taxiExtraInfoStat
      }

    case GetTaxiExtraInfoStat(statId) =>
      sender() ! state
    case DeleteTaxiExtraInfo(statId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiExtraInfoEvent(statId)) { _ =>
        state = state.copy(deletedFlag = true)
      }
    case GetTotalExtraInfoLoaded =>
      //sender() ! statExtraInfoMap.size
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat) =>
      log.info(s"Recovering Extra Info Stat $taxiExtraInfoStat")
      state = taxiExtraInfoStat
    case TaxiExtraInfoStatUpdatedEvent(statId,taxiExtraInfoStat) =>
      log.info(s"Recovering Updated Extra Info Stat $taxiExtraInfoStat")
      state = taxiExtraInfoStat
    case DeletedTaxiExtraInfoEvent(statId) =>
      log.info(s"Recovering Deleted Extra Info Stat")
      state = state.copy(deletedFlag = true)

  }
}


