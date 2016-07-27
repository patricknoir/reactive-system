package org.patricknoir.kafka.reactive.server

import io.circe.{ Decoder, Encoder }
import cats.data._
import org.patricknoir.kafka.reactive.common.{ ReactiveSerializer, ReactiveDeserializer }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class ReactiveRoute(services: Map[String, ReactiveService[_, _]] = Map.empty[String, ReactiveService[_, _]]) {
  def add[In, Out](reactiveService: ReactiveService[In, Out]): ReactiveRoute =
    this.copy(services = services + (reactiveService.id -> reactiveService))
  def ~(reactiveRoute: ReactiveRoute) = ReactiveRoute(this.services ++ reactiveRoute.services)
}

object ReactiveRoute {

  object request {
    def apply[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Future[Error Xor Out]): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(f))
    def sync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => (Error Xor Out)): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future.successful(f(in))))
    def aSync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => (Error Xor Out))(implicit ec: ExecutionContext): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future(f(in))))
  }
  implicit def unsafe[Out: ReactiveSerializer](out: => Out): (Error Xor Out) = Xor.fromTry(Try(out)).leftMap(thr => new Error(thr))

}

