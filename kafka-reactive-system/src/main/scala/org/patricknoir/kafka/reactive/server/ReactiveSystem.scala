package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import cats.data.EitherT
import cats.instances.all._
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseEnvelope, KafkaResponseStatusCode }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope

import scala.concurrent.Future
import scala.util.Try

case class ReactiveSystem(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])(implicit system: ActorSystem) {

  //TODO: replace this naive implementation with a real fully featured one (error handling etc...)

  import system.dispatcher

  val g = source.map { request =>
    val result: Future[String] = for {
      serviceId <- Future.successful(extractServiceId(request))
      service <- Try(route.services(serviceId)).fold(Future.failed[ReactiveService[_, _]](_), Future.successful(_))
      response <- service.unsafeApply(request.payload)
    } yield response
    result
      .map(KafkaResponseEnvelope(request.correlationId, request.replyTo, _, KafkaResponseStatusCode.Success))
      .recover {
        case err: Throwable => KafkaResponseEnvelope(request.correlationId, request.replyTo, err.getMessage, KafkaResponseStatusCode.InternalServerError)
      }
  }.toMat(sink)(Keep.right)

  private def extractServiceId(request: KafkaRequestEnvelope): String = request.destination.serviceId

  def run()(implicit materializer: Materializer) = g.run()
}

