package com.tudux.taxi.http.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

//TODO X: Return error from persistence layer in the HTTP layer (Pablo Patiño)
//TODO X: Rename validateRequestForDecision naming (Pablo Patiño)
class MainRouter(shardedCostActor : ActorRef, shardedExtraInfoActor : ActorRef,
                 shardedPassengerActor : ActorRef, shardedTimeInfoActor : ActorRef, serviceActor : ActorRef)(implicit system: ActorSystem)
  {

  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(2.seconds)

  val taxiTripRoutes = CommonTaxiTripRoutes(shardedCostActor,shardedExtraInfoActor,shardedPassengerActor,shardedTimeInfoActor)
  val costRoutes = CostRoutes(shardedCostActor)
  val timeRoutes = TimeRoutes(shardedTimeInfoActor)
  val passengerRoutes = PassengerRoutes(shardedPassengerActor)
  val extraInfoRoutes = ExtraInfoRoutes(shardedExtraInfoActor)
  val serviceRoutes = ServiceRoutes(serviceActor)
  //val actorInfoRoutes = ActorInfoRoutes(taxiTripActor)
  val pingRoutes = Ping()

  val routes: Route = {
    taxiTripRoutes.routes ~
    costRoutes.routes ~
    timeRoutes.routes ~
    passengerRoutes.routes ~
    extraInfoRoutes.routes  ~
    serviceRoutes.routes ~
    //actorInfoRoutes.routes ~
    pingRoutes.routes


  }
}
