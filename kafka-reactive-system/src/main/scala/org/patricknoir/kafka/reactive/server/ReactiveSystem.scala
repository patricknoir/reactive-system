package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.Materializer
import akka.stream.scaladsl._
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope, KafkaResponseStatusCode }

import scala.concurrent.Future
import scala.util.Try

/**
 * A Runnable instance of a Reactive System Server
 * Concrete instances should be created using the companion object methods:
 * [[ReactiveSystem.atMostOnce]] and [[ReactiveSystem.atLeastOnce]]
 */
trait ReactiveSystem {
  /**
   * Used to start a server instance
   *
   * @param materializer used by the underlying implementation based on Akka Stream
   * @return
   */
  def run()(implicit materializer: Materializer): Any
}

/** Provides the methods to create concrete instances of `ReactiveSystem` */
object ReactiveSystem {

  /**
   * Reactive System server using message delivery semantic at-most-once
   *
   * @param source a valid akka stream source created using [[org.patricknoir.kafka.reactive.server.streams.ReactiveKafkaSource]]
   * @param route
   * @param sink
   * @param system
   * @return
   */
  def apply(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])(implicit system: ActorSystem) = atMostOnce(source, route, sink)

  def atMostOnce(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])(implicit system: ActorSystem) = new ReactiveSystem {

    import system.dispatcher

    def extractServiceId(request: KafkaRequestEnvelope): String = request.destination.serviceId

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

    override def run()(implicit materializer: Materializer) = g.run()(materializer)
  }

  def atLeastOnce(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _], route: ReactiveRoute, sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _])(implicit system: ActorSystem) = new ReactiveSystem {

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

    override def run()(implicit materializer: Materializer) = g.run()(materializer)
  }

}
