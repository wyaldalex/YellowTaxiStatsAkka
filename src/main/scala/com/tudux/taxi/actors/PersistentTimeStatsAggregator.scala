package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

//commands
sealed trait TimeAggregatorCommand
object TimeAggregatorCommand  {
  case class AddTimeAggregatorValues(time: TaxiTripTimeInfoStat)
  case object GetAverageTripTime extends TimeAggregatorCommand
  case class UpdateTimeAggregatorValues(preTime: TaxiTripTimeInfoStat,newTime: TaxiTripTimeInfoStat)
}
//events
sealed trait TimeAggregatorEvent
object TimeAggregatorEvent {
  case class AddedTimeAggregatorValuesEvent(time: TaxiTripTimeInfoStat) extends TimeAggregatorEvent
  case class UpdatedTimeAggregatorValuesEvent(preTime: TaxiTripTimeInfoStat,newTime: TaxiTripTimeInfoStat)
}

//responses
sealed trait TimeAggregatorResponse
object TimeAggregatorResponse{
case class TaxiTripAverageTimeMinutesResponse(averageTimeMinutes: Double) extends TimeAggregatorResponse
}

object PersistentTimeStatsAggregator {
  def props(id: String) : Props = Props(new PersistentTimeStatsAggregator(id))
}
class PersistentTimeStatsAggregator(id: String) extends PersistentActor with ActorLogging{

  import TimeAggregatorCommand._
  import TimeAggregatorResponse._
  import TimeAggregatorEvent._

  val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:SS")
  def getMinutes (taxiTripTimeInfoStat: TaxiTripTimeInfoStat) : Int = {
    ((format.parse(taxiTripTimeInfoStat.tpepDropoffDatetime).getTime - format.parse(taxiTripTimeInfoStat.tpepPickupDatetime).getTime)/60000).toInt
  }

  var totalMinutesTrip : Double = 0
  var totalTrips : Int = 0

  override def persistenceId: String = id
  override def receiveCommand: Receive = {
    case AddTimeAggregatorValues(taxiStat) =>
      log.info("Adding time aggregator values")
      persist(AddedTimeAggregatorValuesEvent(taxiStat)) { _ =>
        val tripMinutes = getMinutes(taxiStat)
        totalMinutesTrip += tripMinutes
        totalTrips += 1
      }
    case UpdateTimeAggregatorValues(preTime,newTime) =>
      log.info("Updating time aggregator values")
      persist(UpdatedTimeAggregatorValuesEvent(preTime, newTime)) { _ =>
        val timeDelta = getMinutes(newTime) - getMinutes(preTime)
        totalMinutesTrip += timeDelta
      }

    case GetAverageTripTime =>
      sender() ! TaxiTripAverageTimeMinutesResponse(totalMinutesTrip / totalTrips)

  }

  override def receiveRecover: Receive = {
    case AddedTimeAggregatorValuesEvent(taxiStat) =>
      val tripMinutes = getMinutes(taxiStat)
      totalMinutesTrip += tripMinutes
      totalTrips += 1

    case UpdatedTimeAggregatorValuesEvent(preTime, newTime) =>
      val timeDelta = getMinutes(newTime) - getMinutes(preTime)
      totalMinutesTrip += timeDelta
  }
}
