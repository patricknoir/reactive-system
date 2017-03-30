package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ ActorLogging, Props }
import akka.stream.actor.ActorPublisher
import org.patricknoir.kafka.reactive.client.actors.protocol._

/**
 * Created by josee on 29/03/2017.
 */
class StreamPublisherActor() extends ActorPublisher[StreamRequestWithSender] with ActorLogging {

  override def receive = {
    case req: StreamRequest =>
      log.debug("Request received: {}", req)
      onNext(StreamRequestWithSender(sender, req))
  }

}

object StreamPublisherActor {
  lazy val props: Props = Props(new StreamPublisherActor)
}