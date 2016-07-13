package org.patricknoir.kafka.reactive.server

import io.circe.{ Error, Decoder, Encoder }
import io.circe.syntax._
import io.circe.parser._
import cats.std.all._
import cats.data._
import scala.concurrent.{ ExecutionContext, Future }

case class ReactiveService[-In: Decoder, +Out: Encoder](id: String)(f: In => Future[Error Xor Out]) {
  def apply(in: In): Future[Error Xor Out] = f(in)

  def unsafeApply(jsonStr: String)(implicit ec: ExecutionContext): Future[Error Xor String] = (for {
    in <- XorT(Future.successful(decode[In](jsonStr)))
    out <- XorT(f(in)).map(_.asJson.noSpaces)
  } yield out).value
}

object ReactiveService {
  implicit def reactiveServiceToRoute(service: ReactiveService[_, _]) = ReactiveRoute().add(service)
}
