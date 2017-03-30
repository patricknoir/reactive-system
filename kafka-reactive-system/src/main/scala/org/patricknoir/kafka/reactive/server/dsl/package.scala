package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.{ Sink, Source }
import org.patricknoir.kafka.reactive.common.{ KafkaResponseEnvelope, KafkaRequestEnvelope }
import org.patricknoir.kafka.reactive.common.{ ReactiveDeserializer, ReactiveSerializer }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Created by patrick on 09/08/2016.
 */
package object dsl {

  object request {
    def apply[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Future[Out]): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(f))
    def sync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future.successful(f(in))))
    def aSync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out)(implicit ec: ExecutionContext): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future(f(in))))
  }
  implicit def unsafe[Out: ReactiveSerializer](out: => Out): (Throwable Either Out) = Try(out).toEither

  implicit class ReactiveSourceShape(source: Source[KafkaRequestEnvelope, _])(implicit system: ActorSystem) {
    def via(route: ReactiveRoute): ReactiveSourceRouteShape = new ReactiveSourceRouteShape(source, route)
    def ~>(route: ReactiveRoute): ReactiveSourceRouteShape = via(route)

    def to(sinkShape: ReactiveSinkShape): ReactiveSystem = ReactiveSystem(source, sinkShape.route, sinkShape.sink)
    def ~>(sinkShape: ReactiveSinkShape): ReactiveSystem = to(sinkShape)
  }

  implicit class ReactiveSourceAtLeastOnceShape(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _])(implicit system: ActorSystem) {
    def via(route: ReactiveRoute): ReactiveSourceRouteAtLeastOnceShape = new ReactiveSourceRouteAtLeastOnceShape(source, route)
    def ~>(route: ReactiveRoute): ReactiveSourceRouteAtLeastOnceShape = via(route)

    def to(sinkShape: ReactiveSinkAtLeastOnceSinkShape): ReactiveSystem = ReactiveSystem.atLeastOnce(source, sinkShape.route, sinkShape.sink)
    def ~>(sinkShape: ReactiveSinkAtLeastOnceSinkShape): ReactiveSystem = to(sinkShape)
  }

  class ReactiveSourceRouteShape(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute)(implicit system: ActorSystem) {
    def to(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = ReactiveSystem(source, route, sink)
    def ~>(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = to(sink)
  }

  class ReactiveSourceRouteAtLeastOnceShape(source: Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _], route: ReactiveRoute)(implicit system: ActorSystem) {
    def to(sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _]): ReactiveSystem = ReactiveSystem.atLeastOnce(source, route, sink)
    def ~>(sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _]): ReactiveSystem = to(sink)
  }

  case class ReactiveSinkShape(route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])
  case class ReactiveSinkAtLeastOnceSinkShape(route: ReactiveRoute, sink: Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _])

}
