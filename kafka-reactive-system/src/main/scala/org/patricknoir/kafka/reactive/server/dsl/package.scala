package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Created by patrick on 09/08/2016.
 */
package object dsl {

  object request {
    def apply[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Future[Out]): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(f))
    def sync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id) { in =>
        Try(f(in)) match {
          case Success(result) => Future.successful(result)
          case Failure(err)    => Future.failed(err)
        }
      })
    def aSync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Out)(implicit ec: ExecutionContext): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future(f(in))))
  }
  implicit def unsafe[Out: ReactiveSerializer](out: => Out): Try[Out] = Try(out)

  implicit class ReactiveSourceShape(source: Source[KafkaRequestEnvelope, _])(implicit system: ActorSystem) {
    def via(route: ReactiveRoute): ReactiveSourceRouteShape = new ReactiveSourceRouteShape(source, route)
    def ~>(route: ReactiveRoute): ReactiveSourceRouteShape = via(route)

    def to(sinkShape: ReactiveSinkShape): ReactiveSystem = ReactiveSystem(source, sinkShape.route, sinkShape.sink)
    def ~>(sinkShape: ReactiveSinkShape): ReactiveSystem = to(sinkShape)
  }

  class ReactiveSourceRouteShape(source: Source[KafkaRequestEnvelope, _], route: ReactiveRoute)(implicit system: ActorSystem) {
    def to(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = ReactiveSystem(source, route, sink)
    def ~>(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSystem = to(sink)
  }

  case class ReactiveSinkShape(route: ReactiveRoute, sink: Sink[Future[KafkaResponseEnvelope], _])

}
