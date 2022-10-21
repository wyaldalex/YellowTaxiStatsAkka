package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.GetTaxiTripPassengerInfo
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.ExecutionContext

case class PassengerRoutes(taxiTripActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiPassengerInfoProtocol
{
  val routes: Route = {
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
      }
  }
}
