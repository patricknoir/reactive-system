package org.patricknoir.kafka.examples.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.patricknoir.kafka.reactive.server.{ ReactiveRoute, ReactiveSystem }
import org.patricknoir.kafka.reactive.server.streams.{ ReactiveKafkaSink, ReactiveKafkaSource }
import scala.concurrent.duration._

import scala.concurrent.Future

/**
 * Created by patrick on 09/08/2016.
 */
object SimpleRSServer extends App {

  implicit val config = EmbeddedKafkaConfig(zooKeeperPort = 2181, kafkaPort = 9092)
  EmbeddedKafka.start()

  //#route-dsl-example

  import org.patricknoir.kafka.reactive.server.dsl._

  implicit val system = ActorSystem("SimpleService")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val route: ReactiveRoute = request.aSync[String, String]("echo") {
    in => println(s"received: $in"); s"echoing: $in"
  } ~ request.aSync("size") { (in: String) =>
    in.length
  } ~ request.aSync("reverse") { (in: String) =>
    in.reverse
  }
  //#route-dsl-example

  //#run-reactive-system

  val source = ReactiveKafkaSource.create("simple", Set("localhost:9092"), "simpleService", "serviceGroup", 8)
  val sink = ReactiveKafkaSink.create(Set("localhost:9092"), 8)

  /**
   * DSL:
   *  val reactiveSys: ReactiveSystem = source via route to sink
   *  val reactiveSys: ReactiveSystem = ReactiveSystem(source, route, sink)
   */
  val reactiveSys: ReactiveSystem = source ~> route ~> sink

  reactiveSys.run()
  //#run-reactive-system
}

object RServerExamples {

  def executeFunctionAsyncExample() = {
    //#route-example-make-async

    import org.patricknoir.kafka.reactive.server.dsl._

    implicit val system = ActorSystem("SimpleService")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    var counter: Int = 0

    def getCounter(): Int = counter
    def incrementCounter(step: Int): Unit = counter += step

    val route: ReactiveRoute = request.aSync[Unit, Int]("getCounter") { _ =>
      getCounter()
    }
    //#route-example-make-async
  }

  def executeFunctionSyncExample() = {
    //#route-example-make-sync

    import org.patricknoir.kafka.reactive.server.dsl._

    implicit val system = ActorSystem("SimpleService")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    var counter: Int = 0

    def getCounter(): Int = counter
    def incrementCounter(step: Int): Unit = counter += step

    val route: ReactiveRoute = request.aSync[Unit, Int]("getCounter") { _ =>
      getCounter()
    } ~ request.sync[Int, Unit]("incrementCounter") { step =>
      incrementCounter(step)
    }
    //#route-example-make-sync
  }

  def executeFunctionExample() = {
    //#route-example-wrap-async

    import org.patricknoir.kafka.reactive.server.dsl._

    implicit val system = ActorSystem("SimpleService")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    var counter: Int = 0

    def getCounter(): Future[Int] = Future(counter)
    def incrementCounter(step: Int): Unit = counter += step

    val route: ReactiveRoute = request[Unit, Int]("getCounter") { _ =>
      getCounter()
    }
    //#route-example-wrap-async
  }

  def createReactiveSystemAtLeastOnce(): Unit = {
    //#reactive-system-at-least-once

    import org.patricknoir.kafka.reactive.server.dsl._

    implicit val system = ActorSystem("SimpleService")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    var counter: Int = 0

    def getCounter(): Int = counter
    def incrementCounter(step: Int): Unit = counter += step

    val route: ReactiveRoute = request.aSync[Unit, Int]("getCounter") { _ =>
      getCounter()
    } ~ request.sync[Int, Unit]("incrementCounter") { step =>
      incrementCounter(step)
    }

    val atLeastOnceSource = ReactiveKafkaSource.atLeastOnce(
      requestTopic = "simple",
      bootstrapServers = Set("localhost:9092"),
      clientId = "simpleService")
    val atLeastOnceSink = ReactiveKafkaSink.atLeastOnce(
      bootstrapServers = Set("localhost:9092"),
      concurrency = 8,
      commitMaxBatchSize = 10,
      commitTimeWindow = 5 seconds)

    val reactiveSystem = atLeastOnceSource ~> route ~> atLeastOnceSink

    reactiveSystem.run()

    //#reactive-system-at-least-once
  }
}