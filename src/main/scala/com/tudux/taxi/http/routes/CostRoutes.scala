package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class CostRoutes(shardedCostActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext, timeout: Timeout ) extends SprayJsonSupport
  with TaxiCostStatProtocol
  with OperationResponseProtocol
{

  def updateTaxiTripCostResponse(tripId: String, request: UpdateCostInfoRequest): Future[OperationResponse] = {
    (shardedCostActor ? request.toCommand(tripId)).mapTo[OperationResponse]
  }

  val routes: Route = {
      pathPrefix("api" / "yellowtaxi" / "cost") {
        get {
          path(Segment) { tripId =>
            println(s"Received some statID $tripId")
            complete(
              // (shardedCostActor ? GetTaxiTripCost(tripId.toString.concat(costActorIdSuffix)))
              (shardedCostActor ? GetTaxiTripCost(tripId.toString))
                // .mapTo[Option[TaxiCostStat]]
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
                  validateRequest(request) {
                    onSuccess(updateTaxiTripCostResponse(tripId,request)) {
                      case operationResponse@OperationResponse(_, status, _) =>
                        // TODO: Akka typed (Major) Classic deprecated
                        // TODO: replace if with pattern matching, trait with case class, compilator checking, missing typed benefits , enforce type safety, ensure compilation checking
                        // val statusCode = if (status == "Failure" ) StatusCodes.BadRequest else StatusCodes.OK // TODO Either, typed exception pattern, string not reliable, defined actor protocol
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

