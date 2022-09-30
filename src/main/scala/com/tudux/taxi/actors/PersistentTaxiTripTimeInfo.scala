package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiTripTimeInfoStat(tpep_pickup_datetime: String,tpep_dropoff_datetime: String,deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoStatCommand {
  case class CreateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoCommand
  case class GetTaxiTimeInfoStat(statId: String) extends TaxiTripTimeInfoCommand
  case class UpdateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoCommand
  case class DeleteTaxiTripTimeInfoStat(statId: String) extends TaxiTripTimeInfoCommand
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoStatCreatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoStatUpdatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
  case class DeletedTaxiTripTimeInfoStatEvent(statId: String) extends TaxiTripTimeInfoEvent
}

object PersistentTaxiTripTimeInfo {
  def props(id: String): Props = Props(new PersistentTaxiTripTimeInfo(id))
}
class PersistentTaxiTripTimeInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripTimeInfoStatCommand._
  import TaxiTripTimeInfoStatEvent._

  //Persistent Actor State
  var taxiTripTimeInfoStatMap : Map[String,TaxiTripTimeInfoStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripTimeInfoStat(statId,taxiTripTimeInfoStat) =>
      persist(TaxiTripTimeInfoStatCreatedEvent(statId,taxiTripTimeInfoStat)) { _ =>
        log.info(s"Creating Trip Time Info Stat $taxiTripTimeInfoStat")
        taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTripTimeInfoStat)
      }
    case UpdateTaxiTripTimeInfoStat(statId, taxiTripTimeInfoStat) =>
      log.info("Updating Time Info ")
      if(taxiTripTimeInfoStatMap.contains(statId)) {
        taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTripTimeInfoStat)
      } else log.info(s"Entry not found to update by id $statId")
    case GetTaxiTimeInfoStat(statId) =>
      sender() ! taxiTripTimeInfoStatMap.get(statId)
    case DeleteTaxiTripTimeInfoStat(statId) =>
      log.info("Deleting taxi cost stat")
      if (taxiTripTimeInfoStatMap.contains(statId)) {
        persist(DeletedTaxiTripTimeInfoStatEvent(statId)) { _ =>
          val taxiTimeInfoDeleted: TaxiTripTimeInfoStat = taxiTripTimeInfoStatMap(statId).copy(deletedFlag = true)
          taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTimeInfoDeleted)
        }
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripTimeInfoStatCreatedEvent(statId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Trip Time Info Stat $taxiTripTimeInfoStat")
      taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTripTimeInfoStat)
    case TaxiTripTimeInfoStatUpdatedEvent(statId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Update Trip Time Info Stat $taxiTripTimeInfoStat")
      taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTripTimeInfoStat)
    case DeletedTaxiTripTimeInfoStatEvent(statId) =>
      log.info(s"Recovering Deleted Trip Time Info Stat ")
      val taxiTimeInfoDeleted: TaxiTripTimeInfoStat = taxiTripTimeInfoStatMap(statId).copy(deletedFlag = true)
      taxiTripTimeInfoStatMap = taxiTripTimeInfoStatMap + (statId -> taxiTimeInfoDeleted)

  }
}


