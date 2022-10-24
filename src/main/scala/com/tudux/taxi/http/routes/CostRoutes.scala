package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class CostRoutes(shardedCostActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout ) extends SprayJsonSupport
  with TaxiCostStatProtocol
{
  val routes: Route = {
      pathPrefix("api" / "yellowtaxi" / "cost") {
        get {
          path(Segment) { tripId =>
            println(s"Received some statID $tripId")
            complete(
              //(shardedCostActor ? GetTaxiTripCost(tripId.toString.concat(costActorIdSuffix)))
              (shardedCostActor ? GetTaxiTripCost(tripId.toString))
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
               val validatedRequestResponse = validateRequest2(request ,
                    {
                    complete(StatusCodes.OK)
                    }
                  )
                  if (validatedRequestResponse.flag) shardedCostActor ! request.toCommand(tripId)
                  validatedRequestResponse.routeResult
                }
              }
            }
          }
      }
  }
}

