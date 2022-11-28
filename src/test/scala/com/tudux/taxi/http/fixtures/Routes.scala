package com.tudux.taxi.http.fixtures

import akka.actor.ActorRef
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.PersistentTaxiTripCost
import com.tudux.taxi.actors.extrainfo.PersistentTaxiExtraInfo
import com.tudux.taxi.actors.passenger.PersistentTaxiTripPassengerInfo
import com.tudux.taxi.actors.service.ServiceActor
import com.tudux.taxi.actors.timeinfo.PersistentTaxiTripTimeInfo
import com.tudux.taxi.http.routes.MainRouter
import org.scalatest._

import scala.concurrent.duration._

trait Routes extends ScalatestRouteTest {
  this: Suite =>

  // Initializing timers
  implicit val timeoutRouteTestTimeout = RouteTestTimeout(60.seconds.dilated)
  implicit val timeout: Timeout = Timeout(30.seconds)

  // Create the aggregators
  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator")
    , "cost-aggregator")
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator")
    , "time-aggregator")

  // Create the persistent actors
  val persistentCost: ActorRef = system.actorOf(PersistentTaxiTripCost.props(costAggregatorActor))
  val persistentExtraInfo: ActorRef = system.actorOf(PersistentTaxiExtraInfo.props)
  val persistentPassenger: ActorRef = system.actorOf(PersistentTaxiTripPassengerInfo.props)
  val persistentTimeInfo: ActorRef = system.actorOf(PersistentTaxiTripTimeInfo.props(timeAggregatorActor))

  val serviceActor: ActorRef = system.actorOf(ServiceActor.props(costAggregatorActor, timeAggregatorActor),
    "serviceActor")

  //routes
  val routes = new MainRouter(persistentCost, persistentExtraInfo,
    persistentPassenger, persistentTimeInfo, serviceActor).routes
}
