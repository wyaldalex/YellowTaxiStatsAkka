package com.tudux.taxi.app

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.{CostActorShardingSettings, PersistentTaxiTripCost}
import com.tudux.taxi.actors.extrainfo.{ExtraInfoActorShardingSettings, PersistentTaxiExtraInfo}
import com.tudux.taxi.actors.passenger.{PassengerInfoActorShardingSettings, PersistentTaxiTripPassengerInfo}
import com.tudux.taxi.actors.service.ServiceActor
import com.tudux.taxi.actors.timeinfo.{PersistentTaxiTripTimeInfo, TimeInfoActorShardingSettings}
import com.tudux.taxi.http.routes.MainRouter
import com.tudux.taxi.http.swagger.Swagger
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//docker exec -it yellotaxistatsakka-cassandra-1 cqlsh
//Visit Swagger Documentation: http://localhost:10001/swagger-ui/index.html

//TODO Review 2: Use scalastyle plugin (Sebastian) (Minor)
//TODO Review 2: Use Cluster sharding to avoid reference map (Agustin Bettati) (Major)
object TaxiApp extends App {

  def startHttpServer(shardedParentCostActor: ActorRef,
                      shardedParentExtraInfoActor: ActorRef ,
                      shardedParentPassengerInfoActor: ActorRef ,
                      shardedParentTimeInfoActor: ActorRef ,
                      serviceActor: ActorRef)(implicit system: ActorSystem): Unit = {
    implicit val scheduler: ExecutionContext = system.dispatcher

    val router = new MainRouter(shardedParentCostActor,shardedParentExtraInfoActor,
      shardedParentPassengerInfoActor, shardedParentTimeInfoActor,serviceActor)
    val routes = router.routes  ~ Swagger(system).routes ~ getFromResourceDirectory("swagger-ui")

    val bindingFuture = Http().newServerAt("localhost", 10001).bind(routes)

    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")

      case Failure(exception) =>
        system.log.error(s"Failed to bing HTTP server, because: $exception")
        system.terminate()
    }
  }
  implicit val timeout: Timeout = Timeout(2.seconds)

  //testing sharding of cost actor
  val config = ConfigFactory.parseString(
    "akka.remote.artery.canonical.port = 2551".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  implicit val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  import ShardedActorsGenerator._
  //Create the aggregators
  val costAggregatorActor : ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor : ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

  //Create the sharded version of the persistent actors
  val persistentCostShardRegionRef: ActorRef = createShardedCostActor(system,costAggregatorActor)
  val persistentExtraInfoShardedRegionRef : ActorRef = createShardedExtraInfoActor(system)
  val persistentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val persistentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system,timeAggregatorActor)

  //Specific Service actor
  val serviceActor : ActorRef = system.actorOf(ServiceActor.props(costAggregatorActor, timeAggregatorActor), "serviceActor")

  startHttpServer(persistentCostShardRegionRef,persistentExtraInfoShardedRegionRef ,
    persistentPassengerShardRegionRef, persistentTimeInfoShardRegionRef,serviceActor)

  /*
  /All working, but need to solve for get:
  22:02:45.036 [YellowTaxiCluster-akka.actor.default-dispatcher-22] WARN akka.cluster.sharding.ShardRegion - OysterCardValidator: Message does not have an extractor defined in shard so it was ignored: GetTaxiTripCost(49d57011-5c99-4ee2-881a-c31e5e2cacbc)
22:02:47.060 [YellowTaxiCluster-akka.actor.default-dispatcher-22] ERROR akka.actor.ActorSystemImpl - Error during processing of request: 'Ask timed out on
   */

}

object ShardedActorsGenerator {

  def createShardedCostActor(system: ActorSystem,costAggregator: ActorRef) : ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedCostActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentTaxiTripCost.props(costAggregator),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = CostActorShardingSettings.extractEntityId,
      extractShardId = CostActorShardingSettings.extractShardId
    )
  }

  def createShardedExtraInfoActor(system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedExtraInfoActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentTaxiExtraInfo.props,
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = ExtraInfoActorShardingSettings.extractEntityId,
      extractShardId = ExtraInfoActorShardingSettings.extractShardId
    )
  }

  def createShardedPassengerInfoActor(system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedPassengerInfoActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentTaxiTripPassengerInfo.props,
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = PassengerInfoActorShardingSettings.extractEntityId,
      extractShardId = PassengerInfoActorShardingSettings.extractShardId
    )
  }

  def createShardedTimeInfoActor(system: ActorSystem, timeAggregator: ActorRef): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedTimeInfoActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentTaxiTripTimeInfo.props(timeAggregator),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = TimeInfoActorShardingSettings.extractEntityId,
      extractShardId = TimeInfoActorShardingSettings.extractShardId
    )
  }

}
