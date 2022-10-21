package com.tudux.taxi.app

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiTripActor
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.cost.PersistentParentTaxiCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.GetTaxiTripCost
import com.tudux.taxi.http.routes.MainRouter
import com.tudux.taxi.http.swagger.Swagger
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//Visit Swagger Documentation: http://localhost:10001/swagger-ui/index.html
object TaxiApp extends App {

  def startHttpServer(taxiAppActor: ActorRef, shardedParentCostActor: ActorRef)(implicit system: ActorSystem): Unit = {
    implicit val scheduler: ExecutionContext = system.dispatcher

    val router = new MainRouter(taxiAppActor,shardedParentCostActor)
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
  object CostActorShardingSettings {

    val numberOfShards = 10 // use 10x number of nodes in your cluster
    val numberOfEntities = 100 //10x number of shards
    //this help to map the corresponding message to a respective entity
    val extractEntityId: ShardRegion.ExtractEntityId = {
      case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat,statId) =>
        val entityId = statId.hashCode.abs % numberOfEntities
        (entityId.toString, createTaxiTripCommand)
      case msg@GetTaxiTripCost(statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        (shardId.toString,msg)
    }

    //this help to map the corresponding message to a respective shard
    val extractShardId: ShardRegion.ExtractShardId = {
      case CreateTaxiTripCommand(taxiStat,statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        shardId.toString
      case GetTaxiTripCost(statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        shardId.toString
      case ShardRegion.StartEntity(entityId) =>
        (entityId.toLong % numberOfShards).toString
    }

  }

  //Somehow create the cost actor sharded version
  val parentCostShardRegionRef: ActorRef = ClusterSharding(system).start(
    typeName = "ShardedParentCostActor",
    //entityProps = Props[PersistentParentTaxiCost],
    entityProps = PersistentParentTaxiCost.props("parent-cost"),
    settings = ClusterShardingSettings(system).withRememberEntities(true),
    extractEntityId = CostActorShardingSettings.extractEntityId,
    extractShardId = CostActorShardingSettings.extractShardId
  )

  val taxiAppActor = system.actorOf(TaxiTripActor.props(parentCostShardRegionRef), "taxiParentAppActor")

  startHttpServer(taxiAppActor,parentCostShardRegionRef)

  /*
  /All working, but need to solve for get:
  22:02:45.036 [YellowTaxiCluster-akka.actor.default-dispatcher-22] WARN akka.cluster.sharding.ShardRegion - OysterCardValidator: Message does not have an extractor defined in shard so it was ignored: GetTaxiTripCost(49d57011-5c99-4ee2-881a-c31e5e2cacbc)
22:02:47.060 [YellowTaxiCluster-akka.actor.default-dispatcher-22] ERROR akka.actor.ActorSystemImpl - Error during processing of request: 'Ask timed out on
   */

}
