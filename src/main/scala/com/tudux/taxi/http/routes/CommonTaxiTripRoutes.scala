package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.DeleteTaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.DeleteTaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.DeleteTaxiTripPassenger
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.DeleteTaxiTripTimeInfo
import com.tudux.taxi.http.formatters.RouteFormatters.CombineCreationResponseProtocol
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class CommonTaxiTripRoutes(costActor: ActorRef, extraInfoActor: ActorRef,
                                passengerActor: ActorRef, timeActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext, timeout: Timeout) extends SprayJsonSupport
  with CombineCreationResponseProtocol {

  def createTaxiTripResponse(request: CreateTaxiTripRequest): Future[CombinedTaxiTripOperationResponse] = {
    val tripId = UUID.randomUUID().toString
    val costResponseFuture: Future[OperationResponse] = (costActor ? request.toCostCommand(tripId)).mapTo[OperationResponse]
    val extraInfoResponseFuture: Future[OperationResponse] = (extraInfoActor ? request.toExtraInfoCommand(tripId)).mapTo[OperationResponse]
    val passengerResponseFuture: Future[OperationResponse] = (passengerActor ? request.toPassengerInfoCommand(tripId)).mapTo[OperationResponse]
    val timeResponseFuture: Future[OperationResponse] = (timeActor ? request.toTimeInfoCommand(tripId)).mapTo[OperationResponse]
    val combineResponse = for {
      r1 <- costResponseFuture
      r2 <- extraInfoResponseFuture
      r3 <- passengerResponseFuture
      r4 <- timeResponseFuture
    } yield CombinedTaxiTripOperationResponse(r1, r2, r3, r4)
    combineResponse
  }

  def deleteTaxiTripResponse(tripId: String): Future[CombinedTaxiTripOperationResponse] = {
    val costResponseFuture: Future[OperationResponse] = (costActor ? DeleteTaxiTripCost(tripId)).mapTo[OperationResponse]
    val extraInfoResponseFuture: Future[OperationResponse] = (extraInfoActor ? DeleteTaxiTripExtraInfo(tripId)).mapTo[OperationResponse]
    val passengerResponseFuture: Future[OperationResponse] = (passengerActor ? DeleteTaxiTripPassenger(tripId)).mapTo[OperationResponse]
    val timeResponseFuture: Future[OperationResponse] = (timeActor ? DeleteTaxiTripTimeInfo(tripId)).mapTo[OperationResponse]
    val combineResponse = for {
      r1 <- costResponseFuture
      r2 <- extraInfoResponseFuture
      r3 <- passengerResponseFuture
      r4 <- timeResponseFuture
    } yield CombinedTaxiTripOperationResponse(r1, r2, r3, r4)
    combineResponse
  }

  val routes: Route = {
    pathPrefix("api" / "yellowtaxi" / "taxitrip") {
      post {
        entity(as[CreateTaxiTripRequest]) { request =>
          validateRequest(request) {
            onSuccess(createTaxiTripResponse(request)) {
              case combineResponse@CombinedTaxiTripOperationResponse(costResponse, extraInfoResponse, passengerResponse, timeResponse) =>
                //val status = if(costResponse.status == "Failure" || extraInfoResponse.status == "Failure" || passengerResponse.status == "Failure" || timeResponse.status == "Failure" ) StatusCodes.BadRequest  else StatusCodes.Created
                val status = (costResponse.status, extraInfoResponse.status, passengerResponse.status,timeResponse.status) match  {
                  case (Right(_), Right(_), Right(_),Right(_)) =>
                    StatusCodes.Created
                  case _ =>
                    StatusCodes.BadRequest
                }
                complete(HttpResponse(
                  status,
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    combineResponse.toJson.prettyPrint
                  )))
            }
          }
        }
      } ~
        delete {
          path(Segment) { tripId =>
            onSuccess(deleteTaxiTripResponse(tripId)) {
              case combineResponse@CombinedTaxiTripOperationResponse(costResponse, extraInfoResponse, passengerResponse, timeResponse) =>
                //val status = if (costResponse.status == "Failure" || extraInfoResponse.status == "Failure" || passengerResponse.status == "Failure" || timeResponse.status == "Failure") StatusCodes.BadRequest else StatusCodes.OK
                val status = (costResponse.status, extraInfoResponse.status, passengerResponse.status, timeResponse.status) match {
                  case (Right(_), Right(_), Right(_), Right(_)) =>
                    StatusCodes.Created
                  case _ =>
                    StatusCodes.BadRequest
                }
                complete(HttpResponse(
                  status,
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    combineResponse.toJson.prettyPrint
                  )))
            }
          }
        }
    }
  }
}
