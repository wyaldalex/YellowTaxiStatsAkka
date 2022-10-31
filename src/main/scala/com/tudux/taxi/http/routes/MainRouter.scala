package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

//TODO Review 2: Return error from persistence layer in the HTTP layer and avoid false positive response (Pablo Patiño)
//TODO Review 2: Rename validateRequest2 naming (Pablo Patiño)
//TODO Review 2: Stop overusing 1 actor to forward everything, each route group should use its own specifc actor (Agustin Bettati)
class MainRouter(shardedCostActor : ActorRef, shardedExtraInfoActor : ActorRef,
                 shardedPassengerActor : ActorRef, shardedTimeInfoActor : ActorRef, serviceActor : ActorRef)(implicit system: ActorSystem)
  {

  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(30.seconds)

  val taxiTripRoutes = CommonTaxiTripRoutes(shardedCostActor,shardedExtraInfoActor,shardedPassengerActor,shardedTimeInfoActor)
  val costRoutes = CostRoutes(shardedCostActor)
  val timeRoutes = TimeRoutes(shardedTimeInfoActor)
  val passengerRoutes = PassengerRoutes(shardedPassengerActor)
  val extraInfoRoutes = ExtraInfoRoutes(shardedExtraInfoActor)
  val serviceRoutes = ServiceRoutes(serviceActor)
  val pingRoutes = Ping()

  val routes: Route = {
    taxiTripRoutes.routes ~
    costRoutes.routes ~
    timeRoutes.routes ~
    passengerRoutes.routes ~
    extraInfoRoutes.routes  ~
    serviceRoutes.routes ~
    pingRoutes.routes


  }
}
