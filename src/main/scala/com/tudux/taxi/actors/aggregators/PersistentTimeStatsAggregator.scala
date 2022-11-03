package com.tudux.taxi.actors.aggregators

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo

//commands
sealed trait TimeAggregatorCommand
object TimeAggregatorCommand  {
  case class AddTimeAggregatorValues(tripId: String,time: TaxiTripTimeInfo) extends TimeAggregatorCommand
  case object GetAverageTripTime extends TimeAggregatorCommand
  case class UpdateTimeAggregatorValues(tripId: String,preTime: TaxiTripTimeInfo, newTime: TaxiTripTimeInfo) extends TimeAggregatorCommand
}
//events
sealed trait TimeAggregatorEvent
object TimeAggregatorEvent {
  case class AddedTimeAggregatorValuesEvent(time: TaxiTripTimeInfo) extends TimeAggregatorEvent
  case class UpdatedTimeAggregatorValuesEvent(preTime: TaxiTripTimeInfo, newTime: TaxiTripTimeInfo) extends TimeAggregatorEvent
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
  import TimeAggregatorEvent._
  import TimeAggregatorResponse._

  val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:SS")
  def getMinutes (taxiTripTimeInfoStat: TaxiTripTimeInfo) : Int = {
    ((format.parse(taxiTripTimeInfoStat.tpepDropoffDatetime).getTime - format.parse(taxiTripTimeInfoStat.tpepPickupDatetime).getTime)/60000).toInt
  }

  var totalMinutesTrip : Double = 0
  var totalTrips : Int = 0
  var tripsWithoutCheckpoint = 0
  val maxMessages = 900

  override def persistenceId: String = id

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error("persist failure being triggered")
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage))
    super.onPersistFailure(cause, event, seqNr)
  }

  override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error("persist rejected being triggered")
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage))
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveCommand: Receive = {
    case AddTimeAggregatorValues(tripId,taxiStat) =>
      log.info("Adding time aggregator values")
      persist(AddedTimeAggregatorValuesEvent(taxiStat)) { _ =>
        val tripMinutes = getMinutes(taxiStat)
        totalMinutesTrip += tripMinutes
        totalTrips += 1
        sender() ! OperationResponse(tripId,Right("Success"))
        maybeCheckpoint()
      }
    case UpdateTimeAggregatorValues(tripId,preTime,newTime) =>
      log.info("Updating time aggregator values")
      persist(UpdatedTimeAggregatorValuesEvent(preTime, newTime)) { _ =>
        val timeDelta = getMinutes(newTime) - getMinutes(preTime)
        totalMinutesTrip += timeDelta
        sender() ! OperationResponse(tripId,Right("Success"))
        maybeCheckpoint()
      }

    case GetAverageTripTime =>
      sender() ! TaxiTripAverageTimeMinutesResponse(totalMinutesTrip / totalTrips)

    //SNAPSHOT related
    case SaveSnapshotSuccess(metadata) =>
      log.info(s"saving snapshot succeeded: $metadata")
    case SaveSnapshotFailure(metadata, reason) =>
      log.warning(s"saving snapshot $metadata failed because of $reason")

  }

  override def receiveRecover: Receive = {
    case AddedTimeAggregatorValuesEvent(taxiStat) =>
      val tripMinutes = getMinutes(taxiStat)
      totalMinutesTrip += tripMinutes
      totalTrips += 1

    case UpdatedTimeAggregatorValuesEvent(preTime, newTime) =>
      val timeDelta = getMinutes(newTime) - getMinutes(preTime)
      totalMinutesTrip += timeDelta
    case SnapshotOffer(metadata, contents) =>
      log.info(s"Recovered snapshot: $metadata")
      val snapState = contents.asInstanceOf[Tuple2[Int,Double]]
      totalTrips = snapState._1
      totalMinutesTrip = snapState._2
  }
  def maybeCheckpoint(): Unit = {
    tripsWithoutCheckpoint += 1
    if (tripsWithoutCheckpoint >= maxMessages) {
      log.info("Saving checkpoint...")
      saveSnapshot((totalTrips -> totalMinutesTrip)) // save a tuple with the current actor state
      tripsWithoutCheckpoint = 0
    }
  }
}
