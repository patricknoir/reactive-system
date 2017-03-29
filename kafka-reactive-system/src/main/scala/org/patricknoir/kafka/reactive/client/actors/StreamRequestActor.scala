package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ Actor, ActorRef, Props, Status }
import akka.util.Timeout
import org.patricknoir.kafka.reactive.common.{ KafkaResponseEnvelope, ReactiveDeserializer }

/**
 * Created by josee on 29/03/2017.
 */
class StreamRequestActor(origin: ActorRef, timeout: Timeout, deserializer: ReactiveDeserializer[_]) extends Actor {

  context.setReceiveTimeout(timeout.duration)

  def receive = {
    case responseEvenlope: KafkaResponseEnvelope =>
      val eitherResult = deserializer.deserialize(responseEvenlope.response.getBytes)
      eitherResult match {
        case Right(response) => origin ! response
        case Left(err)       => origin ! Status.Failure(err)
      }
      context stop self
    case Timeout =>
      origin ! Status.Failure(new RuntimeException("Timeout exception"))
      context stop self
  }

}

object StreamRequestActor {
  def props(origin: ActorRef, timeout: Timeout, deserializer: ReactiveDeserializer[_]): Props = Props(new StreamRequestActor(origin, timeout, deserializer))
}