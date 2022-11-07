package com.tudux.taxi.actors.loader

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Framing, GraphDSL, RunnableGraph, Sink}
import akka.util.{ByteString, Timeout}
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.CreateTaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.CreateTaxiTripExtraInfo
import com.tudux.taxi.actors.implicits.TaxiTripImplicits._
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.CreateTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.CreateTaxiTripTimeInfo
import com.tudux.taxi.app.ShardedActorsGenerator.{createShardedCostActor, createShardedExtraInfoActor, createShardedPassengerInfoActor, createShardedTimeInfoActor}
import com.typesafe.config.ConfigFactory

import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class TaxiTripEntry(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                         tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                         storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                         paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                         tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)

sealed trait TaxiTripCommand
object TaxiTripCommand {
  case class CreateTaxiTripCommand(taxiTrip: TaxiTripEntry, tripId: String = "") extends  TaxiTripCommand
}

object ActorCounter {
  def props(name: String): Props = Props(new ActorCounter(name))
}
class ActorCounter(name: String) extends Actor with ActorLogging {
  var counter = 1

  override def receive: Receive = {
    case operationResponse: OperationResponse =>
      log.info(s"Loader Counter $name received processing response: $counter with values ${operationResponse.toString}")
      counter += 1
  }
}
object TaxiStatAppGraphDslLoader extends App {

  import com.tudux.taxi.reader.CSVConversions._

  val config = ConfigFactory.parseString(
    "akka.remote.artery.canonical.port = 2551".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  implicit val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  implicit val timeout: Timeout = Timeout(20.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher

  val costAggregatorActor: ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor: ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

  val parentCostShardRegionRef: ActorRef = createShardedCostActor(system, costAggregatorActor)
  val parentExtraInfoShardRegionRef: ActorRef = createShardedExtraInfoActor(system)
  val parentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val parentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system, timeAggregatorActor)

  val costCounter: ActorRef =  system.actorOf(ActorCounter.props("CostLoaderCounter"))
  val extraInfoCounter: ActorRef =  system.actorOf(ActorCounter.props("ExtraInfoLoaderCounter"))
  val passengerCounter: ActorRef =  system.actorOf(ActorCounter.props("PassengerLoaderCounter"))
  val timeCounter: ActorRef =  system.actorOf(ActorCounter.props("TimeLoaderCounter"))

  val file = Paths.get("src/main/resources/1ksample.csv")
  // val file = Paths.get("src/main/resources/10ksample.csv")
  //val file = Paths.get("src/main/resources/100ksample.csv")

  val fileSource = FileIO.fromPath(file)

  val toStringFlow = Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String)
  val toTaxiEntryFlow = Flow[String].map(fromCsvEntryToCaseClass)
  val addCommonUUIDFlow = Flow[TaxiTripEntry].map(addUUID)

  val costSink = Sink.foreach[OperationResponse](x => costCounter ! x)
  val extraInfoSink = Sink.foreach[OperationResponse](x => extraInfoCounter ! x)
  val passengerSink = Sink.foreach[OperationResponse](x => passengerCounter ! x)
  val timeInfoSink = Sink.foreach[OperationResponse](x => timeCounter ! x)

  val costFlow = Flow[(String,TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentCostShardRegionRef ? CreateTaxiTripCost(event._1,event._2)).mapTo[OperationResponse]).to(costSink)
  val extraInfoFlow = Flow[(String,TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentExtraInfoShardRegionRef ?  CreateTaxiTripExtraInfo(event._1,event._2)).mapTo[OperationResponse]).to(extraInfoSink)
  val passengerFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentPassengerShardRegionRef ? CreateTaxiTripPassengerInfo(event._1,event._2)).mapTo[OperationResponse]).to(passengerSink)
  val timeFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentTimeInfoShardRegionRef ? CreateTaxiTripTimeInfo(event._1,event._2)).mapTo[OperationResponse]).to(timeInfoSink)

  val graph = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val bcast = builder.add(Broadcast[(String,TaxiTripEntry)](4))
      fileSource ~> toStringFlow ~> toTaxiEntryFlow ~> addCommonUUIDFlow ~> bcast.in
      bcast.out(0) ~> costFlow
      bcast.out(1) ~> extraInfoFlow
      bcast.out(2) ~> passengerFlow
      bcast.out(3) ~> timeFlow

      ClosedShape
  })
  graph.run()
}
