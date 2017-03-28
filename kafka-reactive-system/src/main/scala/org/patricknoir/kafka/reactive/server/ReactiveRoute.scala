package org.patricknoir.kafka.reactive.server

import akka.stream.scaladsl.Sink
import io.circe.{ Decoder, Encoder }
import cats.data._
import org.patricknoir.kafka.reactive.common.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import org.patricknoir.kafka.reactive.server.dsl.ReactiveSinkShape
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Used to define the Services exposed by a [[ReactiveSystem]] server instance
 *
 * @param services services exposed by this server instance indexed by service id
 */
case class ReactiveRoute(services: Map[String, ReactiveService[_, _]] = Map.empty[String, ReactiveService[_, _]]) {
  def add[In, Out](reactiveService: ReactiveService[In, Out]): ReactiveRoute =
    this.copy(services = services + (reactiveService.id -> reactiveService))

  /**
   * Used to merge multpile routes
   * @param reactiveRoute
   * @return a new route which is the merge of the current instance with the passed reactiveRoute parameter
   */
  def ~(reactiveRoute: ReactiveRoute) = ReactiveRoute(this.services ++ reactiveRoute.services)

}

