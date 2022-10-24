package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.GetTaxiTripExtraInfo
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.ExecutionContext

case class ExtraInfoRoutes(shardedExtraInfoActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiExtraInfoProtocol
{

  val routes: Route = {
      pathPrefix("api" / "yellowtaxi" / "extrainfo") {
        get {
          path(Segment) { tripId =>
            println(s"Received some statID $tripId")
            complete(
              (shardedExtraInfoActor ? GetTaxiTripExtraInfo(tripId.toString))
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
                  shardedExtraInfoActor ! request.toCommand(tripId)
                  complete(StatusCodes.OK)
                }
              }
            }
          }
      }
  }
}

