package com.tudux.taxi.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.CostAggregatorCommand.{CalculateTripDistanceCost, GetAverageTipAmount}
import com.tudux.taxi.actors.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.TaxiTripResponse.TaxiTripCreatedResponse
import com.tudux.taxi.actors.TaxiTripCommand.DeleteTaxiTrip
import com.tudux.taxi.actors.TimeAggregatorCommand.GetAverageTripTime
import com.tudux.taxi.actors.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.{GetTaxiTripExtraInfo, GetTotalExtraInfoLoaded}
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.{GetTaxiTripPassengerInfo, GetTotalPassengerInfoLoaded}
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.{GetTaxiTripTimeInfo, GetTotalTimeInfoInfoLoaded}
import com.tudux.taxi.http.RouteHelpers._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}




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
  implicit val timeout: Timeout = Timeout(2.seconds)

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val routes: Route = {
    pathPrefix("api" / "yellowtaxi" / "taxitrip") {
        post {
            entity(as[CreateTaxiTripRequest]) { request =>
              val statCreatedFuture: Future[TaxiTripCreatedResponse] = (taxiTripActor ? request.toCommand).mapTo[TaxiTripCreatedResponse]
              println(s"Received http post to create stat $request")
              complete(statCreatedFuture.map { r =>
                HttpResponse(
                  StatusCodes.Created,
                  entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    s"Taxi Trip created with Id: ${r.tripId.toString}"
                  )
                )
              })
            }
        } ~
        delete {
          path(Segment) { tripId =>
            taxiTripActor ! DeleteTaxiTrip(tripId)
            complete(StatusCodes.OK)
          }
        }
    } ~
    pathPrefix("api" / "yellowtaxi" / "cost") {
      get {
        path(Segment) { tripId =>
          println(s"Received some statID $tripId")
          complete(
            (taxiTripActor ? GetTaxiTripCost(tripId.toString))
              //.mapTo[Option[TaxiCostStat]]
              .mapTo[TaxiTripCost]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { tripId =>
          put {
            entity(as[UpdateCostInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(tripId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "time") {
      get {
        path(Segment) { tripId =>
          println(s"Received some statID $tripId")
          complete(
            (taxiTripActor ? GetTaxiTripTimeInfo(tripId.toString))
             // .mapTo[Option[TaxiTripTimeInfoStat]]
              .mapTo[TaxiTripTimeInfo]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { tripId =>
          put {
            entity(as[UpdateTimeInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(tripId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "passenger") {
      get {
        path(Segment) { tripId =>
          println(s"Received some statID $tripId")
          complete(
            (taxiTripActor ? GetTaxiTripPassengerInfo(tripId.toString))
              .mapTo[TaxiTripPassengerInfo]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { tripId =>
          put {
            entity(as[UpdatePassengerInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(tripId)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~
    pathPrefix("api" / "yellowtaxi" / "extrainfo") {
      get {
        path(Segment) { tripId =>
          println(s"Received some statID $tripId")
          complete(
            (taxiTripActor ? GetTaxiTripExtraInfo(tripId.toString))
              //.mapTo[Option[TaxiExtraInfoStat]]
              .mapTo[TaxiTripExtraInfo]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      put {
        path(Segment) { tripId =>
          put {
            entity(as[UpdateExtraInfoRequest]) { request =>
              taxiTripActor ! request.toCommand(tripId)
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
      pathPrefix("api" / "yellowtaxi" /  "actor" / "loaded") {
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
