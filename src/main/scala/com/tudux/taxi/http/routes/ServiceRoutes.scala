package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.CostAggregatorCommand.{CalculateTripDistanceCost, GetAverageTipAmount}
import com.tudux.taxi.actors.aggregators.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand.GetAverageTripTime
import com.tudux.taxi.actors.aggregators.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.http.formatters.RouteFormatters._
import com.tudux.taxi.http.payloads.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.ExecutionContext

case class ServiceRoutes(taxiTripActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout )extends SprayJsonSupport
  with CalculateDistanceCostProtocol
  with CalculateAverageTripTimeProtocol
  with GetAverageTipAmountProtocol
  with GetTotalLoadedResponseProtocol
{
  val routes: Route = {
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
      }
  }
}

