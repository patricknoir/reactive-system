package org.patricknoir.kafka.reactive.client.actors

import akka.actor._
import akka.event.LoggingReceive
import cats.data.Xor
import io.circe.Decoder
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseStatusCode, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import io.circe.parser._

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRRequestActor(producer: ActorRef) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case KafkaRequest(destination, payload, timeout, replyTo, decoder) =>
      producer ! KafkaRequestEnvelope(self.path.toString, destination, payload, replyTo)
      context.setReceiveTimeout(timeout.duration)
      context.become(waitingResponse(sender, decoder))
  }

  def waitingResponse(client: ActorRef, decoder: Decoder[_]): Receive = LoggingReceive {
    case KafkaResponseEnvelope(_, response, KafkaResponseStatusCode.Success) =>
      client ! decode(response)(decoder)
      context stop self
    case KafkaResponseEnvelope(_, response, _) =>
      client ! Xor.left(new Error(response))
      context stop self
    case ReceiveTimeout => context stop self
  }

}

object KafkaRRequestActor {
  def props(producer: ActorRef) = Props(new KafkaRRequestActor(producer))
}
