package org.patricknoir.kafka.reactive.server

import org.patricknoir.kafka.reactive.common.serializer._
import org.patricknoir.kafka.reactive.common.deserializer._
import cats.data._
import cats.instances.all._
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import scala.concurrent.{ ExecutionContext, Future }

case class ReactiveService[-In: ReactiveDeserializer, +Out: ReactiveSerializer](id: String)(f: In => Future[Out]) {
  def apply(in: In): Future[Out] = f(in)

  def unsafeApply(jsonStr: String)(implicit ec: ExecutionContext): Future[String] = {
    val xorData = deserialize[In](jsonStr)
    xorData match {
      case Xor.Right(in) => f(in).map(serialize[Out])
      case Xor.Left(err) => Future.failed(new Exception(err.getMessage))
    }
  }

}

object ReactiveService {
  implicit def reactiveServiceToRoute(service: ReactiveService[_, _]) = ReactiveRoute().add(service)
}
