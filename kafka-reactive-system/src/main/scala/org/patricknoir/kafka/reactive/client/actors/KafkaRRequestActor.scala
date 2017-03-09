package org.patricknoir.kafka.reactive.client.actors

import akka.actor._
import akka.event.LoggingReceive
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseStatusCode, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import org.patricknoir.kafka.reactive.common.ReactiveDeserializer

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRRequestActor(producer: ActorRef) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case r @ KafkaRequest(destination, payload, timeout, replyTo, decoder) =>
      producer ! KafkaRequestEnvelope(self.path.toString, destination, payload, replyTo)
      context.setReceiveTimeout(timeout.duration)
      context.become(waitingResponse(sender, decoder))
  }

  def waitingResponse(client: ActorRef, decoder: ReactiveDeserializer[_]): Receive = LoggingReceive {
    case resp @ KafkaResponseEnvelope(_, _, response, KafkaResponseStatusCode.Success) =>
      client ! decoder.deserialize(response.getBytes)
      context stop self
    case KafkaResponseEnvelope(_, _, response, _) =>
      client ! Left(new Error(response))
      context stop self
    case ReceiveTimeout => context stop self
  }

}

object KafkaRRequestActor {
  def props(producer: ActorRef) = Props(new KafkaRRequestActor(producer))
}
