package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Validated
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.loader.TaxiTripCommand.DeleteTaxiTrip
import com.tudux.taxi.actors.loader.TaxiTripResponse.TaxiTripCreatedResponse
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.{CreateTaxiTripCost, DeleteTaxiTripCost}
import com.tudux.taxi.actors.extrainfo.TaxiExtraInfoResponse.TaxiTripExtraInfoCreatedResponse
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.DeleteTaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.DeleteTaxiTripPassenger
import com.tudux.taxi.actors.passenger.TaxiTripPassengerResponse.TaxiTripPassengerResponseCreated
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.DeleteTaxiTripTimeInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeResponse.TaxiTripTimeResponseCreated
import com.tudux.taxi.http.formatters.RouteFormatters.CombineCreationResponseProtocol
import com.tudux.taxi.http.payloads.RoutePayloads._
import com.tudux.taxi.http.validation.Validation.{Validator, validateEntity}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class CommonTaxiTripRoutes(costActor: ActorRef, extraInfoActor: ActorRef,
                                passengerActor: ActorRef, timeActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
                          with CombineCreationResponseProtocol
{
//  def createTaxiTripCreatedResponse(createTaxiTripRequest: CreateTaxiTripRequest,tripId: String): TaxiCreatedResponse = {
////    println(s"Received http post to create stat $createTaxiTripRequest")
////    (taxiTripActor ? createTaxiTripRequest.toCommand).mapTo[TaxiTripCreatedResponse]
//    val costActorResponse = (costActor ? createTaxiTripRequest.toCostCommand(tripId: String)).mapTo[TaxiCostCreatedResponse]
//    TaxiCreatedResponse(costActorResponse,costActorResponse,costActorResponse,costActorResponse)
//  }

  val routes: Route = {
    pathPrefix("api" / "yellowtaxi" / "taxitrip") {
      post {
//        entity(as[CreateTaxiTripRequest]) { request =>
//          validateRequest(request) {
//            val tripId = UUID.randomUUID().toString
//            onSuccess(createTaxiTripCostResponse(request,tripId)) {
//              case TaxiCreatedResponse(tripIdResult,tripIdResult2,tripIdResult3,tripIdResult4) =>
//                complete(HttpResponse(
//                  StatusCodes.Created,
//                  entity = HttpEntity(
//                    ContentTypes.`text/html(UTF-8)`,
//                    s"Taxi Trip created with Id: ${tripIdResult.toString}"
//                  )
//                ))
//            }
        entity(as[CreateTaxiTripRequest]) { request =>
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
          } yield CombineCreationResponse(r1, r2, r3, r4)
          complete(
            combineResponse.mapTo[CombineCreationResponse]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )

        }
      } ~
        delete {
          path(Segment) { tripId =>
            //taxiTripActor ! DeleteTaxiTrip(tripId)
            costActor ! DeleteTaxiTripCost(tripId)
            extraInfoActor ! DeleteTaxiTripExtraInfo(tripId)
            passengerActor ! DeleteTaxiTripPassenger(tripId)
            timeActor ! DeleteTaxiTripTimeInfo(tripId)
            complete(StatusCodes.OK)
          }
        }
    }
  }
}
