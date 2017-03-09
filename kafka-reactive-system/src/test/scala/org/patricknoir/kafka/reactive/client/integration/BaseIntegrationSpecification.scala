package org.patricknoir.kafka.reactive.client.integration

import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import akka.util.Timeout
import cats.data.Xor
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{ EmbeddedKafkaConfig, EmbeddedKafka }
import org.patricknoir.kafka.KafkaLocal
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.server.ReactiveSystem
import org.patricknoir.kafka.reactive.server.streams.{ ReactiveKafkaSink, ReactiveKafkaSource }
import org.specs2.SpecificationLike
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import akka.stream.scaladsl._
import org.patricknoir.kafka.reactive.server.dsl._

/**
 * Created by patrick on 16/07/2016.
 */
abstract class BaseIntegrationSpecification extends TestKit(ActorSystem("TestKit", ConfigFactory.parseString(
  """
    |akka {
    |  log-config-on-start = off
    |
    |  loggers = ["akka.testkit.TestEventListener"]
    |  loglevel = "DEBUG"
    |  stdout-loglevel = "DEBUG"
    |
    |  logger-startup-timeout = 10s
    |  jvm-exit-on-fatal-error = off
    |
    |  log-dead-letters = on
    |  log-dead-letters-during-shutdown = on
    |
    |  actor {
    |    debug {
    |      autoreceive = on
    |      receive = on
    |      lifecycle = on
    |      fsm = on
    |      event-stream = on
    |      unhandled = on
    |    }
    |
    |    custom {
    |      dispatchers {
    |        bounded-fork-join-dispatcher {
    |          type = Dispatcher
    |          executor = "fork-join-executor"
    |          mailbox-requirement = "akka.dispatch.BoundedMessageQueueSemantics"
    |        }
    |      }
    |    }
    |  }
    |}
  """.stripMargin
))) with SpecificationLike {

  var kafka = Option.empty[KafkaLocal]

  def startUp() = {
    val kkProperties = new Properties()
    val zkProperties = new Properties()
    kkProperties.load(this.getClass.getClassLoader.getResourceAsStream("server.properties"))
    zkProperties.load(this.getClass.getClassLoader.getResourceAsStream("zookeeper.properties"))
    kafka = Some(new KafkaLocal(kkProperties, zkProperties))
  }

  def shutDwon() = {
    kafka.foreach(_.stop)
  }

}

class SimpleIntegrationSpecification extends BaseIntegrationSpecification {

  def is = s2"""

    base test  $simpleTest

  """

  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9092, zooKeeperPort = 2181)
  implicit val materializer = ActorMaterializer()

  def startServer() = {

    val echoService = new KafkaEchoService()
    echoService.run()
  }

  def before() = {
    EmbeddedKafka.start()
    startServer()
  }

  def simpleTest = {
    before()

    implicit val timeout = Timeout(10 seconds)
    val client = new KafkaReactiveClient(KafkaRClientSettings.default)

    val fResponse = client.request[String, String]("kafka:echoInbound/echo", "patrick")

    val Right(result: String) = Await.result(fResponse, Duration.Inf)

    after()
    result must be_==("patrick")
  }

  def after() = {
    materializer.shutdown()
    EmbeddedKafka.stop()
  }
}

class KafkaEchoService(implicit system: ActorSystem, materializer: Materializer) {

  import system.dispatcher

  val source: Source[KafkaRequestEnvelope, _] = ReactiveKafkaSource.create("echoInbound", Set("localhost:9092"), "client1", "group1")
  val route = request.sync[String, String]("echo") { in =>
    println("received request: " + in)
    in
  }

  //  (identity[String])
  val sink: Sink[Future[KafkaResponseEnvelope], _] = ReactiveKafkaSink.create(Set("localhost:9092"))

  val rsys = source ~> route ~> sink

  def run() = rsys.run()

}