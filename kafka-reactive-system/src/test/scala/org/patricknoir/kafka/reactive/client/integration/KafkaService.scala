package org.patricknoir.kafka.reactive.client.integration

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.server.dsl._
import org.patricknoir.kafka.reactive.server.{ ReactiveRoute, ReactiveSystem }
import org.patricknoir.kafka.reactive.server.streams.{ ReactiveKafkaSink, ReactiveKafkaSource }
import scala.concurrent.duration._

import scala.concurrent.Future

object ServiceCatalog {

  val echo = request.sync[String, String]("echo") { in =>
    println("received request: " + in)
    in
  }

}

object KafkaService {
  def atMostOnce(route: ReactiveRoute)(implicit system: ActorSystem): ReactiveSystem = {
    import system.dispatcher
    val source: Source[KafkaRequestEnvelope, _] = ReactiveKafkaSource.create("echoInbound", Set("localhost:9092"), "client1", "group1", 8)
    val sink: Sink[Future[KafkaResponseEnvelope], _] = ReactiveKafkaSink.create(Set("localhost:9092"), 8)
    source ~> route ~> sink
  }

  def atLeastOnce(route: ReactiveRoute)(implicit system: ActorSystem): ReactiveSystem = {
    import system.dispatcher
    val source = ReactiveKafkaSource.atLeastOnce("echoInbound", Set("localhost:9092"), "client1", "group1")
    val sink = ReactiveKafkaSink.atLeastOnce(Set("localhost:9092"), 8, 10, 5 seconds)
    source ~> route ~> sink
  }
}
