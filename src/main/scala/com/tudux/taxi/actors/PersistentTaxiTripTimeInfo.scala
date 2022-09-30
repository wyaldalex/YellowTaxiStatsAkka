package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiTripTimeInfoStat(tpep_pickup_datetime: String,tpep_dropoff_datetime: String,deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoStatCommand {
  case class CreateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoCommand
  case class GetTaxiTimeInfoStat(statId: String)
  case class UpdateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoCommand
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoStatCreatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoStatUpdatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
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
  }
}


