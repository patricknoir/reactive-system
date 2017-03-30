package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.{ Sink, Source }
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope, ReactiveDeserializer, ReactiveSerializer }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Created by patrick on 09/08/2016.
 */
package object dsl {

  class ReactiveSourceRouteShape(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute)(implicit system: ActorSystem) {
    def ~>(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = to(sink)

    def to(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = ReactiveSystem(source, route, sink)
  }

  implicit def unsafe[Out: ReactiveSerializer](out: => Out): (Throwable Either Out) = Try(out).toEither

  implicit class ReactiveSourceShape(source: Source[KafkaRequestEnvelope, _])(implicit system: ActorSystem) {
    def ~>(route: ReactiveRoute): ReactiveSourceRouteShape = via(route)

    def via(route: ReactiveRoute): ReactiveSourceRouteShape = new ReactiveSourceRouteShape(source, route)

    def ~>(sinkShape: ReactiveSinkShape): ReactiveSystem = to(sinkShape)

    def to(sinkShape: ReactiveSinkShape): ReactiveSystem = ReactiveSystem(source, sinkShape.route, sinkShape.sink)
  }

  implicit class ReactiveSourceAtLeastOnceShape(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _])(implicit system: ActorSystem) {
    def ~>(route: ReactiveRoute): ReactiveSourceRouteAtLeastOnceShape = via(route)

    def via(route: ReactiveRoute): ReactiveSourceRouteAtLeastOnceShape = new ReactiveSourceRouteAtLeastOnceShape(source, route)

    def ~>(sinkShape: ReactiveSinkAtLeastOnceSinkShape): ReactiveSystem = to(sinkShape)

    def to(sinkShape: ReactiveSinkAtLeastOnceSinkShape): ReactiveSystem = ReactiveSystem.atLeastOnce(source, sinkShape.route, sinkShape.sink)
  }

  class ReactiveSourceRouteAtLeastOnceShape(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _], route: ReactiveRoute)(implicit system: ActorSystem) {
    def ~>(sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _]): ReactiveSystem = to(sink)

    def to(sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _]): ReactiveSystem = ReactiveSystem.atLeastOnce(source, route, sink)
  }

  case class ReactiveSinkShape(route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])

  case class ReactiveSinkAtLeastOnceSinkShape(route: ReactiveRoute, sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _])

  object request {
    def apply[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Future[Out]): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(f))

    def sync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future.successful(f(in))))

    def aSync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out)(implicit ec: ExecutionContext): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future(f(in))))
  }

}
