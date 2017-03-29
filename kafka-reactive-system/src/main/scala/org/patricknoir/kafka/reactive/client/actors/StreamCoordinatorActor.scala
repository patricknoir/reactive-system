package org.patricknoir.kafka.reactive.client.actors

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, Props }
import org.patricknoir.kafka.reactive.client.actors.protocol._
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope }

import scala.annotation.tailrec

/**
 * Created by josee on 29/03/2017.
 */
class StreamCoordinatorActor() extends Actor with ActorLogging {

  def receive = {
    case reqWithSender: StreamRequestWithSender =>
      handleRequest(reqWithSender)
    case respEnv @ KafkaResponseEnvelope(correlationId, _, _, _) =>
      forwardToChild(correlationId, respEnv)
    case msgSent @ SendMessageComplete(correlationId) =>
      forwardToChild(correlationId, msgSent)
  }

  //FIXME: check it doesn't exists already
  @tailrec
  private def generateUUID(): String = {
    val candidate = UUID.randomUUID().toString()
    context.child(candidate) match {
      case None    => candidate
      case Some(_) => generateUUID()
    }
  }

  private def handleRequest(reqWithSender: StreamRequestWithSender) = {
    val correlationId = generateUUID()
    createActorPerRequest(correlationId, reqWithSender)
    import reqWithSender.request._
    sender ! KafkaRequestEnvelope(correlationId, destination, payload, responseInfo.map(_.replyTo).getOrElse(""))
  }

  private def createActorPerRequest(correlationId: String, reqWithSender: StreamRequestWithSender) = {
    reqWithSender.request.responseInfo.fold(
      context.actorOf(StreamRequestOneWayActor.props(reqWithSender.origin, reqWithSender.request.timeout), correlationId)
    )(respInfo =>
        context.actorOf(StreamRequestActor.props(reqWithSender.origin, reqWithSender.request.timeout, respInfo.deserializer), correlationId))
  }

  private def forwardToChild(correlationId: String, msg: Any) = {
    context.child(correlationId).fold(
      log.warning(s"Unexpected reponse received for: ${correlationId}")
    )(child =>
        child ! msg)
  }
}

object StreamCoordinatorActor {
  def props: Props = Props(new StreamCoordinatorActor())
}