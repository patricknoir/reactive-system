package org.patricknoir.kafka.examples.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.patricknoir.kafka.reactive.server.{ ReactiveSystem, ReactiveRoute }
import org.patricknoir.kafka.reactive.server.dsl._
import org.patricknoir.kafka.reactive.server.streams.{ ReactiveKafkaSink, ReactiveKafkaSource }

/**
 * Created by patrick on 09/08/2016.
 */
object SimpleRSServer extends App {

  implicit val system = ActorSystem("SimpleService")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val route: ReactiveRoute = request.aSync[String, String]("echo") {
    in => s"echoing: $in"
  } ~ request.aSync("size") { (in: String) =>
    in.length
  }

  val source = ReactiveKafkaSource.create("simple", Set("localhost:9092"), "simpleService")
  val sink = ReactiveKafkaSink.create(Set("localhost:9092"))

  /**
   * DSL:
   *  val reactiveSys: ReactiveSystem = source via route to sink
   *  val reactiveSys: ReactiveSystem = ReactiveSystem(source, route, sink)
   */
  val reactiveSys: ReactiveSystem = source ~> route ~> sink

  reactiveSys.run()
}
