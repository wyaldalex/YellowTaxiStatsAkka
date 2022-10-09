package com.tudux.taxi.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiCostStatCommand._
import com.tudux.taxi.actors.TaxiCostStatsResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.TaxiExtraInfoStatCommand.{GetTaxiExtraInfoStat, GetTotalExtraInfoLoaded, UpdateTaxiExtraInfoStat}
import com.tudux.taxi.actors.TaxiStatCommand.{CreateTaxiStat, DeleteTaxiStat}
import com.tudux.taxi.actors.TaxiStatResponseResponses.TaxiStatCreatedResponse
import com.tudux.taxi.actors.TaxiTripPassengerInfoStatCommand.{GetTaxiPassengerInfoStat, GetTotalPassengerInfoLoaded, UpdateTaxiPassenger}
import com.tudux.taxi.actors.TaxiTripTimeInfoStatCommand.{GetAverageTripTime, GetTaxiTimeInfoStat, GetTotalTimeInfoInfoLoaded, UpdateTaxiTripTimeInfoStat}
import com.tudux.taxi.actors.TaxiTripTimeResponses.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors._
import spray.json._

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class CreateTaxiStatRequest(VendorID: Int, tpep_pickup_datetime: String, tpep_dropoff_datetime: String, passenger_count: Int,
                                 trip_distance: Double, pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                                 store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double,
                                 payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                 tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {
  def toCommand: CreateTaxiStat = CreateTaxiStat(TaxiStat(VendorID, tpep_pickup_datetime, tpep_dropoff_datetime, passenger_count,
    trip_distance, pickup_longitude, pickup_latitude, RateCodeID,
    store_and_fwd_flag, dropoff_longitude, dropoff_latitude,
    payment_type, fare_amount, extra, mta_tax,
    tip_amount, tolls_amount, improvement_surcharge, total_amount))
}

case class UpdatePassengerInfoRequest(passenger_count: Int) {
  def toCommand(statId: String) : UpdateTaxiPassenger = UpdateTaxiPassenger(statId,TaxiTripPassengerInfoStat(passenger_count))
}
case class UpdateExtraInfoRequest(pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                                  store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double) {
  def toCommand(statId: String) : UpdateTaxiExtraInfoStat = UpdateTaxiExtraInfoStat(statId,TaxiExtraInfoStat(pickup_longitude,pickup_latitude, RateCodeID,
    store_and_fwd_flag,dropoff_longitude,dropoff_latitude))
}
case class UpdateTimeInfoRequest(tpep_pickup_datetime: String,tpep_dropoff_datetime: String) {
  def toCommand(statId: String) : UpdateTaxiTripTimeInfoStat = UpdateTaxiTripTimeInfoStat(statId,TaxiTripTimeInfoStat(tpep_pickup_datetime,tpep_dropoff_datetime))
}
case class UpdateCostInfoRequest(VendorID: Int,
                                 trip_distance: Double,
                                 payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                 tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {
  def toCommand(statId: String) : UpdateTaxiCostStat = UpdateTaxiCostStat(statId,TaxiCostStat(VendorID,
    trip_distance,
    payment_type, fare_amount, extra, mta_tax,
    tip_amount, tolls_amount, improvement_surcharge, total_amount))
}

case class LoadedStatsResponse(totalCostLoaded: Int, totalExtraInfoLoaded: Int, totalTimeInfoLoaded: Int, totalPassengerInfo: Int)

trait TaxiCostStatProtocol extends DefaultJsonProtocol {
  implicit val taxiCostStatFormat = jsonFormat11(TaxiCostStat)
}

trait TaxiTimeInfoStatProtocol extends DefaultJsonProtocol {
  implicit val taxiTimeInfoFormat = jsonFormat3(TaxiTripTimeInfoStat)
}

trait TaxiPassengerInfoProtocol extends DefaultJsonProtocol {
  implicit val taxiPassengerFormat = jsonFormat2(TaxiTripPassengerInfoStat)
}

trait TaxiExtraInfoProtocol extends DefaultJsonProtocol {
  implicit val taxiExtraInfoFormat = jsonFormat7(TaxiExtraInfoStat)
}

trait CalculateDistanceCostProtocol extends DefaultJsonProtocol {
  implicit val calculateDistanceCostFormat = jsonFormat1(CalculateTripDistanceCostResponse)
}

trait CalculateAverageTripTimeProtocol extends DefaultJsonProtocol {
  implicit val averageTripTimeFormat = jsonFormat1(TaxiTripAverageTimeMinutesResponse)
}
trait GetAverageTipAmountProtocol extends DefaultJsonProtocol {
  implicit val averageTipAmountFormat = jsonFormat1(GetAverageTipAmountResponse)
}
trait GetTotalLoadedResponseProtocol extends DefaultJsonProtocol {
  implicit val totalLoadedResponseFormat = jsonFormat4(LoadedStatsResponse)
}


class TaxiStatsRouter(taxiTripActor: ActorRef)(implicit system: ActorSystem) extends SprayJsonSupport
  with TaxiCostStatProtocol
  with TaxiTimeInfoStatProtocol
  with TaxiPassengerInfoProtocol
  with TaxiExtraInfoProtocol
  with CalculateDistanceCostProtocol
  with CalculateAverageTripTimeProtocol
  with GetAverageTipAmountProtocol
  with GetTotalLoadedResponseProtocol
  {
  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(50.seconds)

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val routes: Route = {
    pathPrefix("api" / "yellowtaxi" / "stat") {
        post {
            entity(as[CreateTaxiStatRequest]) { request =>
              val statCreatedFuture: Future[TaxiStatCreatedResponse] = (taxiTripActor ? request.toCommand).mapTo[TaxiStatCreatedResponse]
              println(s"Received http post to create stat $request")
              complete(statCreatedFuture.map { r =>
                HttpResponse(
                  StatusCodes.Created,
                  entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    s"Stat created with Id ${r.statId.toString}"
                  )
                )
              })
            }
        } ~
        delete {
          path(Segment) { statId =>
            taxiTripActor ! DeleteTaxiStat(statId)
            complete(StatusCodes.OK)
          }
        }
    } ~
    pathPrefix("api" / "yellowtaxi" / "cost") {
      get {
        path(Segment) { statId =>
          println(s"Received some statID $statId")
          complete(
            (taxiTripActor ? GetTaxiCostStat(statId.toString))
              //.mapTo[Option[TaxiCostStat]]
              .mapTo[TaxiCostStat]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { statId =>
          put {
            entity(as[UpdateCostInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(statId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "time") {
      get {
        path(Segment) { statId =>
          println(s"Received some statID $statId")
          complete(
            (taxiTripActor ? GetTaxiTimeInfoStat(statId.toString))
              .mapTo[Option[TaxiTripTimeInfoStat]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { statId =>
          put {
            entity(as[UpdateTimeInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(statId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "passenger") {
      get {
        path(Segment) { statId =>
          println(s"Received some statID $statId")
          complete(
            (taxiTripActor ? GetTaxiPassengerInfoStat(statId.toString))
              .mapTo[Option[TaxiTripPassengerInfoStat]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { statId =>
          put {
            entity(as[UpdatePassengerInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(statId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "extrainfo") {
      get {
        path(Segment) { statId =>
          println(s"Received some statID $statId")
          complete(
            (taxiTripActor ? GetTaxiExtraInfoStat(statId.toString))
              .mapTo[Option[TaxiExtraInfoStat]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { statId =>
          put {
            entity(as[UpdateExtraInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(statId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "service" / "calculate-distance-cost") {
      get {
        path(Segment) { distance =>
          println(s"Calculating average cost for distance: $distance")
          complete(
            (taxiTripActor ? CalculateTripDistanceCost(distance.toDouble))
              .mapTo[CalculateTripDistanceCostResponse]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      }
    } ~
      pathPrefix("api" / "yellowtaxi" / "service" / "average-trip-time") {
        get {
            complete(
              (taxiTripActor ? GetAverageTripTime)
                .mapTo[TaxiTripAverageTimeMinutesResponse]
                .map(_.toJson.prettyPrint)
                .map(toHttpEntity)
            )
        }
      } ~
      pathPrefix("api" / "yellowtaxi" / "service" / "average-tip") {
        get {
          complete(
            (taxiTripActor ? GetAverageTipAmount)
              .mapTo[GetAverageTipAmountResponse]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      pathPrefix("api" / "yellowtaxi" /  "loaded" / "stat") {
        get {
          val statTotalCostLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalCostLoaded).mapTo[Int]
          val statTotalPassengerInfoLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalPassengerInfoLoaded).mapTo[Int]
          val statTotalExtraInfoLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalExtraInfoLoaded).mapTo[Int]
          val statTotalTimeInfoFuture: Future[Int] = (taxiTripActor ? GetTotalTimeInfoInfoLoaded).mapTo[Int]
          val totalLoadResponse = for {
            r1 <- statTotalCostLoadedFuture
            r2 <- statTotalExtraInfoLoadedFuture
            r3 <- statTotalTimeInfoFuture
            r4 <- statTotalPassengerInfoLoadedFuture
          } yield LoadedStatsResponse(r1,r2,r3,r4)
          complete(
            totalLoadResponse.mapTo[LoadedStatsResponse]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      }
  }
}
