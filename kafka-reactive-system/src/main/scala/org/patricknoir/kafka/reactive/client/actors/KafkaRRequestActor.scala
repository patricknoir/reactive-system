package org.patricknoir.kafka.reactive.client.actors

import akka.actor._
import akka.event.LoggingReceive
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseEnvelope, KafkaResponseStatusCode }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.{ KafkaMessage, KafkaRequest }
import org.patricknoir.kafka.reactive.common.ReactiveDeserializer
import akka.pattern.ask
import org.patricknoir.kafka.reactive.client.actors.KafkaRRequestActor.KafkaProducerRequest

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRRequestActor(producer: ActorRef) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case r @ KafkaRequest(destination, payload, timeout, replyTo, decoder) =>
      producer ! KafkaProducerRequest(KafkaRequestEnvelope(self.path.toString, destination, payload, replyTo))
      context.setReceiveTimeout(timeout.duration)
      context.become(waitingResponse(sender, decoder))
    case m @ KafkaMessage(destination, payload, confirmSend) =>
      producer forward KafkaProducerRequest(KafkaRequestEnvelope(self.path.toString, destination, payload, replyTo = ""), confirmSend)
      context stop self
  }

  def waitingResponse(client: ActorRef, decoder: ReactiveDeserializer[_]): Receive = LoggingReceive {
    case resp @ KafkaResponseEnvelope(_, _, response, KafkaResponseStatusCode.Success) =>
      decoder.deserialize(response.getBytes).fold(client ! Status.Failure(_), client ! _)
      context stop self
    case KafkaResponseEnvelope(_, _, response, _) =>
      client ! Status.Failure(new RuntimeException(response))
      context stop self
    case ReceiveTimeout => context stop self
  }

}

object KafkaRRequestActor {
  def props(producer: ActorRef) = Props(new KafkaRRequestActor(producer))

  case class KafkaProducerRequest(request: KafkaRequestEnvelope, confirmSend: Boolean = false)
}
