package org.patricknoir.kafka.reactive.server

import org.patricknoir.kafka.reactive.common.serializer._
import org.patricknoir.kafka.reactive.common.deserializer._
import cats.data._
import cats.instances.all._
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import scala.concurrent.{ ExecutionContext, Future }

case class ReactiveService[-In: ReactiveDeserializer, +Out: ReactiveSerializer](id: String)(f: In => Future[Error Xor Out]) {
  def apply(in: In): Future[Error Xor Out] = f(in)

  def unsafeApply(jsonStr: String)(implicit ec: ExecutionContext): Future[Error Xor String] = (for {
    in <- XorT(Future.successful(deserialize[In](jsonStr)))
    out <- XorT(f(in)).map(out => serialize[Out](out))
  } yield out).value
}

object ReactiveService {
  implicit def reactiveServiceToRoute(service: ReactiveService[_, _]) = ReactiveRoute().add(service)
}
