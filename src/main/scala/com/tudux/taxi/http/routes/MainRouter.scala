package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt


class MainRouter(taxiTripActor: ActorRef)(implicit system: ActorSystem)
  {

  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(2.seconds)

  val taxiTripRoutes = TaxiTripRoutes(taxiTripActor)
  val costRoutes = CostRoutes(taxiTripActor)
  val timeRoutes = TimeRoutes(taxiTripActor)
  val passengerRoutes = PassengerRoutes(taxiTripActor)
  val extraInfoRoutes = ExtraInfoRoutes(taxiTripActor)
  val serviceRoutes = ServiceRoutes(taxiTripActor)
  val actorInfoRoutes = ActorInfoRoutes(taxiTripActor)
  val pingRoutes = Ping()

  val routes: Route = {
    taxiTripRoutes.routes ~
    costRoutes.routes ~
    timeRoutes.routes ~
    passengerRoutes.routes ~
    extraInfoRoutes.routes  ~
    serviceRoutes.routes ~
    actorInfoRoutes.routes ~
    pingRoutes.routes


  }
}
