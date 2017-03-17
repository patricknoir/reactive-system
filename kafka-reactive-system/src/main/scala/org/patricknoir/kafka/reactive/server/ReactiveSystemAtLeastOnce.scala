package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, _ }
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseEnvelope, KafkaResponseStatusCode }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class Concurrency(parallelism: Int)

/**
 * Created by patrick on 14/03/2017.
 */
case class ReactiveSystemAtLeastOnce(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _], route: ReactiveRoute, sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _])(implicit system: ActorSystem) {

  import system.dispatcher

  val g = {
    source.map {
      case (msg, request) =>
        val result: Future[String] = for {
          serviceId <- Future.successful(request.destination.serviceId)
          service <- Future.fromTry(Try(route.services(serviceId)))
          response <- service.unsafeApply(request.payload)
        } yield response

        val f = result
          .map(resp => (KafkaResponseEnvelope(request.correlationId, request.replyTo, resp, KafkaResponseStatusCode.Success)))
          .recover(handleErrors(request.correlationId, request.replyTo))
        (msg, f)
    }.toMat(sink)(Keep.right)
  }

  private def handleErrors(correlationId: String, replyTo: String): PartialFunction[Throwable, KafkaResponseEnvelope] = {
    case generic: Throwable => KafkaResponseEnvelope(correlationId, replyTo, generic.getMessage, KafkaResponseStatusCode.InternalServerError)
  }

  def run()(implicit materializer: Materializer) = g.run()
}
