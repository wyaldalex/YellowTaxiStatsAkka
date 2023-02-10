package com.tudux.taxi.actors.loader

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Framing, GraphDSL, Merge, RunnableGraph, Sink}
import akka.stream.{ClosedShape, IOResult}
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
import org.slf4j.{Logger, LoggerFactory}

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class TaxiTripEntry(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                         tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                         storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                         paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                         tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)


object ActorCounter {

  case class CounterAck(n: Int)

  def props(name: String): Props = Props(new ActorCounter(name))
}

class ActorCounter(name: String) extends Actor with ActorLogging {

  import ActorCounter._

  var counter = 0

  override def receive: Receive = {
    case operationResponse: OperationResponse =>
      counter += 1
      log.info(s"Loader Counter $name received processing response: $counter with values ${operationResponse.toString}")
      sender() ! CounterAck(counter)
    case x: IOResult =>
      log.info(s"IO Source Finished with ${x}")
    case _ =>
      log.info("Other value")
  }
}

object TaxiStatAppGraphDslLoader extends App {
  import com.tudux.taxi.reader.CSVConversions._

  val log: Logger = LoggerFactory.getLogger(TaxiStatAppGraphDslLoader.getClass.getName)

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

  val counterActor: ActorRef =  system.actorOf(ActorCounter.props("loaderCounter"))

  val file = Paths.get("src/main/resources/smallset.csv")
  //val file = Paths.get("src/main/resources/1ksample.csv")
  // val file = Paths.get("src/main/resources/10ksample.csv")
  //val file = Paths.get("src/main/resources/100ksample.csv")

  val fileSource = FileIO.fromPath(file)

  val toStringFlow = Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String)
  val toTaxiEntryFlow = Flow[String].map(fromCsvEntryToCaseClass)
  val addCommonUUIDFlow = Flow[TaxiTripEntry].map(addUUID)

  val costFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentCostShardRegionRef ? CreateTaxiTripCost(event._1, event._2)).mapTo[OperationResponse])
  val extraInfoFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentExtraInfoShardRegionRef ? CreateTaxiTripExtraInfo(event._1, event._2)).mapTo[OperationResponse])
  val passengerFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentPassengerShardRegionRef ? CreateTaxiTripPassengerInfo(event._1, event._2)).mapTo[OperationResponse])
  val timeFlow = Flow[(String, TaxiTripEntry)].mapAsync(parallelism = 1)(event => (parentTimeInfoShardRegionRef ? CreateTaxiTripTimeInfo(event._1, event._2)).mapTo[OperationResponse])

  import ActorCounter.CounterAck
  val counterFlow = Flow[OperationResponse].mapAsync(parallelism = 1)(event => (counterActor ? event).mapTo[CounterAck])
  val simpleSink = Sink.foreach[CounterAck](ackC =>
  {
   log.info(s"Response at the final sink with counter incrementing to ${ackC.n}")
  })


  val graph = RunnableGraph.fromGraph(GraphDSL.create(simpleSink) {
    implicit builder: Builder[Future[Done]] => simpleSink =>
      import GraphDSL.Implicits._
      val bcast = builder.add(Broadcast[(String,TaxiTripEntry)](4))
      val merge = builder.add(Merge[OperationResponse](4))
      fileSource ~> toStringFlow ~> toTaxiEntryFlow ~> addCommonUUIDFlow ~> bcast.in
      bcast.out(0) ~> costFlow ~> merge.in(0)
      bcast.out(1) ~> extraInfoFlow ~> merge.in(1)
      bcast.out(2) ~> passengerFlow  ~> merge.in(2)
      bcast.out(3) ~> timeFlow ~> merge.in(3)
      merge.out ~> counterFlow ~> simpleSink

      ClosedShape
  })

  val matRunnable = graph.run()
  matRunnable.onComplete {
    case Success(value) =>
      log.info(s"Closing GraphDSL Based Loader Application $value")
      system.terminate()
    case Failure(exception) =>
      log.error(s"Error while running Closing GraphDSL Based Loader Application ${exception.getStackTrace}")
      system.terminate()
  }


//  def terminateWhen(done: Future[IOResult]): Unit = {
//    done.onComplete {
//      case Success(_) =>
//        println(s"Flow Success. Written file: $file About to terminate...")
//        system.terminate()
//      case Failure(e) =>
//        println(s"Flow Failure: $e. About to terminate...")
//        system.terminate()
//    }
//  }

}
