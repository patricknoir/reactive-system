package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import cats.std.all._
import cats.data.{ XorT, Xor }
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseStatusCode, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope

import scala.concurrent.Future
import scala.util.Try

case class ReactiveSystem(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])(implicit system: ActorSystem) {

  //TODO: replace this naive implementation with a real fully featured one (error handling etc...)

  import system.dispatcher

  val g = source.map { request =>
    val result: Future[Error Xor String] = (for {
      serviceId <- XorT(Future.successful(extractServiceId(request)))
      service <- XorT(Future.successful(Xor.fromOption[Error, ReactiveService[_, _]](route.services.get(serviceId), new Error(s"service: $serviceId not found"))))
      response <- XorT(service.unsafeApply(request.payload))
    } yield response).value

    result.map {
      case Xor.Left(err: Error) => KafkaResponseEnvelope(request.correlationId, err.getMessage, KafkaResponseStatusCode.InternalServerError)
      case Xor.Right(jsonResp)  => KafkaResponseEnvelope(request.correlationId, jsonResp, KafkaResponseStatusCode.Success)
    }
  }.to(sink)

  private def extractServiceId(request: KafkaRequestEnvelope): Xor[Error, String] = Xor.fromTry(Try {
    request.destination.split("/")(1)
  }).leftMap(throwable => new Error(throwable))

  def run()(implicit materializer: Materializer) = g.run()
}
