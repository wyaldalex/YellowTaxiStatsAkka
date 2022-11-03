package com.tudux.taxi.actors.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object ServiceActor {
  def props(costAggregatorActor : ActorRef, timeAggregatorActor : ActorRef): Props =
    Props(new ServiceActor(costAggregatorActor, timeAggregatorActor))
}
class ServiceActor(costAggregatorActor : ActorRef, timeAggregatorActor : ActorRef) extends Actor with ActorLogging {

  import com.tudux.taxi.actors.aggregators.CostAggregatorCommand._
  import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand._

  override def receive : Receive = {

    // Domain Specific Operations
    case calculateTripDistanceCost@CalculateTripDistanceCost(_) =>
      log.info("Received CalculateTripDistanceCost request")
      costAggregatorActor.forward(calculateTripDistanceCost)
    case getAverageTripTime@GetAverageTripTime =>
      timeAggregatorActor.forward(getAverageTripTime)
    case getAverageTipAmount@GetAverageTipAmount =>
      log.info("Received GetAverageTipAmount request")
      costAggregatorActor.forward(getAverageTipAmount)
    case _ => log.info(s"Received something else at service actor ${self.path.name}")

  }

}
