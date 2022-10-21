package com.tudux.taxi.app

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiTripActor
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.cost.{CostActorShardingSettings, PersistentParentTaxiCost}
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.GetTaxiTripCost
import com.tudux.taxi.actors.extrainfo.{ExtraInfoActorShardingSettings, PersistentParentExtraInfo}
import com.tudux.taxi.http.routes.MainRouter
import com.tudux.taxi.http.swagger.Swagger
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//Visit Swagger Documentation: http://localhost:10001/swagger-ui/index.html
object TaxiApp extends App {

  def startHttpServer(taxiAppActor: ActorRef, shardedParentCostActor: ActorRef,
                      shardedParentExtraInfoActor: ActorRef )(implicit system: ActorSystem): Unit = {
    implicit val scheduler: ExecutionContext = system.dispatcher

    val router = new MainRouter(taxiAppActor,shardedParentCostActor,shardedParentExtraInfoActor)
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
    s"""
       |akka.remote.artery.canonical.port = 2551
  """.stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  implicit val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)

  import ShardedActorsGenerator._
  //Somehow create the cost actor sharded version
  val parentCostShardRegionRef: ActorRef = createShardedParentCostActor(system)
  val parentExtraInfoShardedRegionRef : ActorRef = createShardedParentExtraInfoActor(system)

  val taxiAppActor = system.actorOf(TaxiTripActor.props(
    parentCostShardRegionRef,
    parentExtraInfoShardedRegionRef), "taxiParentAppActor")

  startHttpServer(taxiAppActor,parentCostShardRegionRef,parentExtraInfoShardedRegionRef )

  /*
  /All working, but need to solve for get:
  22:02:45.036 [YellowTaxiCluster-akka.actor.default-dispatcher-22] WARN akka.cluster.sharding.ShardRegion - OysterCardValidator: Message does not have an extractor defined in shard so it was ignored: GetTaxiTripCost(49d57011-5c99-4ee2-881a-c31e5e2cacbc)
22:02:47.060 [YellowTaxiCluster-akka.actor.default-dispatcher-22] ERROR akka.actor.ActorSystemImpl - Error during processing of request: 'Ask timed out on
   */

}

object ShardedActorsGenerator {

  def createShardedParentCostActor(system: ActorSystem) : ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedParentCostActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentParentTaxiCost.props("parent-cost"),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = CostActorShardingSettings.extractEntityId,
      extractShardId = CostActorShardingSettings.extractShardId
    )
  }

  def createShardedParentExtraInfoActor(system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ShardedParentExtraInfoActor",
      //entityProps = Props[PersistentParentTaxiCost],
      entityProps = PersistentParentExtraInfo.props("parent-extra-info"),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractEntityId = ExtraInfoActorShardingSettings.extractEntityId,
      extractShardId = ExtraInfoActorShardingSettings.extractShardId
    )
  }

}
