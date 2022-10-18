package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.GetTotalExtraInfoLoaded
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.GetTotalPassengerInfoLoaded
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.GetTotalTimeInfoInfoLoaded
import com.tudux.taxi.http.helpers.RouteFormatters._
import com.tudux.taxi.http.helpers.RoutePayloads._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class ActorInfoRoutes(taxiTripActor: ActorRef)(implicit system: ActorSystem, dispatcher: ExecutionContext,timeout: Timeout )extends SprayJsonSupport
  with GetTotalLoadedResponseProtocol
{
  val routes: Route = {
      pathPrefix("api" / "yellowtaxi" /  "actor" / "loaded") {
        get {
          val statTotalCostLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalCostLoaded).mapTo[Int]
          val statTotalPassengerInfoLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalPassengerInfoLoaded).mapTo[Int]
          val statTotalExtraInfoLoadedFuture: Future[Int] = (taxiTripActor ? GetTotalExtraInfoLoaded).mapTo[Int]
          val statTotalTimeInfoFuture: Future[Int] = (taxiTripActor ? GetTotalTimeInfoInfoLoaded).mapTo[Int]
          val totalLoadResponse = for {
            r1 <- statTotalCostLoadedFuture
            r2 <- statTotalExtraInfoLoadedFuture
            r3 <- statTotalTimeInfoFuture
            r4 <- statTotalPassengerInfoLoadedFuture
          } yield LoadedStatsResponse(r1,r2,r3,r4)
          complete(
            totalLoadResponse.mapTo[LoadedStatsResponse]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      }
  }
}
