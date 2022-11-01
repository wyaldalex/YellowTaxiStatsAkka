package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.GetTaxiTripPassengerInfo
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class PassengerRoutes(shardedPassengerActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiPassengerInfoProtocol
  with OperationResponseProtocol
{

  def updateTaxiTripPassengerResponse(tripId: String, request: UpdatePassengerInfoRequest): Future[OperationResponse] = {
    (shardedPassengerActor ? request.toCommand(tripId)).mapTo[OperationResponse]
  }

  val routes: Route = {
      pathPrefix("api" / "yellowtaxi" / "passenger") {
        get {
          path(Segment) { tripId =>
            println(s"Received some statID $tripId")
            complete(
              (shardedPassengerActor ? GetTaxiTripPassengerInfo(tripId.toString))
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
                  validateRequest(request) {
                    onSuccess(updateTaxiTripPassengerResponse(tripId, request)) {
                      case operationResponse@OperationResponse(_, status, _) =>
                        //val statusCode = if (status == "Failure") StatusCodes.BadRequest else StatusCodes.OK
                        val statusCode = status match {
                          case Right(_) => StatusCodes.Created
                          case Left(_) => StatusCodes.BadRequest
                        }
                        complete(HttpResponse(
                          statusCode,
                          entity = HttpEntity(
                            ContentTypes.`application/json`,
                            operationResponse.toJson.prettyPrint)))
                    }
                  }
                }
              }
            }
          }
      }
  }
}
