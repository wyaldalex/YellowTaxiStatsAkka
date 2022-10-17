package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Validated
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.GetTaxiTripTimeInfo
import com.tudux.taxi.http.helpers.RouteFormatters._
import com.tudux.taxi.http.helpers.RoutePayloads._
import com.tudux.taxi.http.validation.Validation.{Validator, validateEntity}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.ExecutionContext

case class TimeRoutes(taxiTripActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiTimeInfoStatProtocol
{

  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route = {
    validateEntity(request) match {
      case Validated.Valid(_) => routeIfValid
      case Validated.Invalid(failures) =>
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }
  }

  val routes: Route = {
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
                  val validatedRequestResponse = validateRequest2(request,
                    {
                      complete(StatusCodes.OK)
                    }
                  )
                  if (validatedRequestResponse.flag) taxiTripActor ! request.toCommand(tripId)
                  validatedRequestResponse.routeResult
                }
              }
            }
          }
      }
  }
}
