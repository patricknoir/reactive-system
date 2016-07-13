package org.patricknoir.kafka.reactive.client.actors

import java.util.UUID

import akka.actor.{ ActorRef, Props, Actor, ActorLogging }
import akka.event.LoggingReceive
import akka.util.Timeout
import io.circe.Decoder
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRClientActor(producerProps: Props, consumerProps: Props) extends Actor with ActorLogging {

  //TODO: add supervisor strategy to handle KafkaProducerException and KafkaConsumerException in order to restart
  // kafka consumer and producer

  val producer = context.actorOf(producerProps, "producer")
  val consumer = context.actorOf(consumerProps, "consumer")

  def createRequestActor(): ActorRef = context.actorOf(KafkaRRequestActor.props(producer), s"request-${UUID.randomUUID()}")

  def receive = LoggingReceive {
    case request: KafkaRequest => createRequestActor() forward request
  }

}

object KafkaRClientActor {

  case class KafkaRequest(destination: String, payload: String, timeout: Timeout, replyTo: String, decoder: Decoder[_])

  def props(producerProps: Props, consumerProps: Props) = Props(new KafkaRClientActor(producerProps, consumerProps))
}
