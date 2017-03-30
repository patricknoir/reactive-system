package org.patricknoir.kafka.reactive.client

import akka.actor.ActorRef
import akka.util.Timeout
import org.patricknoir.kafka.reactive.common.{ Destination, ReactiveDeserializer }

package object actors {
  object protocol {
    case class ResponseInfo(replyTo: String, deserializer: ReactiveDeserializer[_])

    case class StreamRequest(destination: Destination, payload: String, timeout: Timeout, responseInfo: Option[ResponseInfo])
    case class StreamRequestWithSender(origin: ActorRef, request: StreamRequest)

    case class SendMessageComplete(correlationId: String)
  }
}
