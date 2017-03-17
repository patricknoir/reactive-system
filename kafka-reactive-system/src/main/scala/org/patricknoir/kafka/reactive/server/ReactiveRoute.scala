package org.patricknoir.kafka.reactive.server

import akka.stream.scaladsl.Sink
import io.circe.{ Decoder, Encoder }
import cats.data._
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import org.patricknoir.kafka.reactive.server.dsl.ReactiveSinkShape
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class ReactiveRoute(services: Map[String, ReactiveService[_, _]] = Map.empty[String, ReactiveService[_, _]]) {
  def add[In, Out](reactiveService: ReactiveService[In, Out]): ReactiveRoute =
    this.copy(services = services + (reactiveService.id -> reactiveService))

  def ~(reactiveRoute: ReactiveRoute) = ReactiveRoute(this.services ++ reactiveRoute.services)

  //  def to(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSinkShape = new ReactiveSinkShape(this, sink)
  //  def ~>(sink: Sink[Future[KafkaResponseEnvelope], _]): ReactiveSinkShape = to(sink)
}

