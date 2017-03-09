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
    val result: Future[Error Either String] = (for {
      serviceId <- EitherT(Future.successful(extractServiceId(request)))
      service <- EitherT(Future.successful(Either.cond[Error, ReactiveService[_, _]](route.services.get(serviceId).isDefined, route.services(serviceId), new Error(s"service: $serviceId not found"))))
      response <- EitherT(service.unsafeApply(request.payload))
    } yield response).value

    result.map(toKafkaResponseEnvelope(request.correlationId, request.replyTo, _))
  }.toMat(sink)(Keep.right)

  private def extractServiceId(request: KafkaRequestEnvelope): Either[Error, String] = Try {
    request.destination.serviceId
  }.toEither.left.map(throwable => new Error(throwable))

  private def toKafkaResponseEnvelope(correlationId: String, origin: String, xor: Either[Error, String]): KafkaResponseEnvelope = xor match {
    case Left(err: Error) => KafkaResponseEnvelope(correlationId, origin, err.getMessage, KafkaResponseStatusCode.InternalServerError)
    case Right(jsonResp)  => KafkaResponseEnvelope(correlationId, origin, jsonResp, KafkaResponseStatusCode.Success)
  }

  def run()(implicit materializer: Materializer) = g.run()
}

