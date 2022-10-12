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
import com.tudux.taxi.actors.cost.TaxiCostStatCommand._
import com.tudux.taxi.actors.extrainfo.TaxiExtraInfoStatCommand.{GetTaxiExtraInfoStat, GetTotalExtraInfoLoaded}
import com.tudux.taxi.actors.TaxiTripCommand.DeleteTaxiStat
import com.tudux.taxi.actors.TaxiStatResponseResponses.TaxiStatCreatedResponse
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoStatCommand.{GetTaxiPassengerInfoStat, GetTotalPassengerInfoLoaded}
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoStatCommand.{GetTaxiTimeInfoStat, GetTotalTimeInfoInfoLoaded}
import com.tudux.taxi.actors.TimeAggregatorCommand.GetAverageTripTime
import com.tudux.taxi.actors.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors._
import com.tudux.taxi.actors.cost.TaxiCostStat
import com.tudux.taxi.actors.extrainfo.TaxiExtraInfoStat
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoStat
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoStat
import com.tudux.taxi.http.RouteHelpers._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt




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
             // .mapTo[Option[TaxiTripTimeInfoStat]]
              .mapTo[TaxiTripTimeInfoStat]
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
              .mapTo[TaxiTripPassengerInfoStat]
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
              //.mapTo[Option[TaxiExtraInfoStat]]
              .mapTo[TaxiExtraInfoStat]
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
