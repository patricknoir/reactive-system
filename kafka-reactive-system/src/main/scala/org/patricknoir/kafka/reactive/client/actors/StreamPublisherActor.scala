package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ ActorLogging, Props }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import org.patricknoir.kafka.reactive.client.actors.protocol._

import scala.collection.mutable

/**
 * Represents the entry point for the client requests into
 * the client stream.
 * Created by josee on 29/03/2017.
 */
class StreamPublisherActor() extends ActorPublisher[StreamRequestWithSender] with ActorLogging {

  private val queue: mutable.Queue[StreamRequestWithSender] = mutable.Queue()

  override def receive = {
    case Request(num) =>
      publishIfNeeded()
    case req: StreamRequest =>
      log.debug("Request received: {}", req)
      queue.enqueue(StreamRequestWithSender(sender, req))
      publishIfNeeded()
    case Cancel =>
      context.stop(self)
  }

  private def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue())
    }
  }

}

object StreamPublisherActor {
  lazy val props: Props = Props(new StreamPublisherActor)
}