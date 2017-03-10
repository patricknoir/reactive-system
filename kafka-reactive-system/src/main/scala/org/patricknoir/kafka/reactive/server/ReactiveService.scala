package org.patricknoir.kafka.reactive.server

import org.patricknoir.kafka.reactive.common.serializer._
import org.patricknoir.kafka.reactive.common.deserializer._
import cats.data._
import cats.instances.all._
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A reactive service is the abstraction of a function uniquely identified by an ID.
 * The service function accept an input type that can be deserialized and produces a
 * Future of an Output that can be serialized.
 * @param id
 * @param f
 * @tparam In
 * @tparam Out
 */
case class ReactiveService[-In: ReactiveDeserializer, +Out: ReactiveSerializer](id: String)(f: In => Future[Out]) {
  def apply(in: In): Future[Out] = f(in)

  def unsafeApply(jsonStr: String)(implicit ec: ExecutionContext): Future[String] = for {
    in <- deserialize[In](jsonStr).fold(thr => Future.failed[In](thr), js => Future.successful(js))
    out <- f(in).map(out => serialize[Out](out))
  } yield out
}

object ReactiveService {
  implicit def reactiveServiceToRoute(service: ReactiveService[_, _]) = ReactiveRoute().add(service)
}
