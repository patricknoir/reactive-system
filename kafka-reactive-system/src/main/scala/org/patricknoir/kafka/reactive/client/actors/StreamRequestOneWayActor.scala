package org.patricknoir.kafka.reactive.client.actors

import akka.Done
import akka.actor.{ Actor, ActorRef, Props, Status }
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.actors.protocol.SendMessageComplete

/**
  * This Actor is used to handle request of type `one-way-message`.
  * Is spawn by the [[StreamCoordinatorActor]] and handles the completion
  * of the Ask Promise with the confirmation of commit of the message into
  * the destination queue by the producer.
 * Created by josee on 29/03/2017.
 */
class StreamRequestOneWayActor(origin: ActorRef, timeout: Timeout) extends Actor {

  context.setReceiveTimeout(timeout.duration)

  override def receive: Receive = {
    case SendMessageComplete(_) =>
      origin ! Done
      context stop self
    case Timeout =>
      origin ! Status.Failure(new RuntimeException("Timeout exception"))
      context stop self
  }
}
object StreamRequestOneWayActor {
  def props(origin: ActorRef, timeout: Timeout): Props = Props(new StreamRequestOneWayActor(origin, timeout))
}
