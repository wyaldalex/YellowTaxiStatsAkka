package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Validated
import com.tudux.taxi.actors.TaxiTripCommand.DeleteTaxiTrip
import com.tudux.taxi.actors.TaxiTripResponse.TaxiTripCreatedResponse
import com.tudux.taxi.http.helpers.RouteFormatters._
import com.tudux.taxi.http.helpers.RoutePayloads._
import com.tudux.taxi.http.validation.Validation.{Validator, validateEntity}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}




case class TaxiTripRoutes(taxiTripActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiCostStatProtocol
  with TaxiTimeInfoStatProtocol
  with TaxiPassengerInfoProtocol
  with TaxiExtraInfoProtocol
  with CalculateDistanceCostProtocol
  with CalculateAverageTripTimeProtocol
  with GetAverageTipAmountProtocol
  with GetTotalLoadedResponseProtocol
{

  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route = {
    validateEntity(request) match {
      case Validated.Valid(_) => routeIfValid
      case Validated.Invalid(failures) =>
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }
  }

  val routes: Route = {
    pathPrefix("api" / "yellowtaxi" / "taxitrip") {
      post {
        entity(as[CreateTaxiTripRequest]) { request =>
          validateRequest(request) {
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
        }
      } ~
        delete {
          path(Segment) { tripId =>
            taxiTripActor ! DeleteTaxiTrip(tripId)
            complete(StatusCodes.OK)
          }
        }
    }
  }
}
