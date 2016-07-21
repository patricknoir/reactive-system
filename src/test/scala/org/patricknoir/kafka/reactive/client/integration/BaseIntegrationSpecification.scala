package org.patricknoir.kafka.reactive.client.integration

import java.util.Properties

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{ Producer, Consumer }
import akka.kafka.{ ProducerSettings, ConsumerSettings }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import akka.util.Timeout
import cats.data.Xor
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{ EmbeddedKafkaConfig, EmbeddedKafka }
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringSerializer, StringDeserializer }
import org.patricknoir.kafka.KafkaLocal
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.server.ReactiveSystem
import org.specs2.SpecificationLike
import org.specs2.mutable.BeforeAfter
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import akka.stream.scaladsl._

import scala.reflect.io.Directory

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

    base test  $simpleTest (#simpleTest) //commented out for now

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

    val Xor.Right(result: String) = Await.result(fResponse, Duration.Inf)

    after()
    result must be_==("patrick")
  }

  def after() = {
    materializer.shutdown()
    EmbeddedKafka.stop()
  }
}

class KafkaEchoService(implicit system: ActorSystem, materializer: Materializer) {

  import org.patricknoir.kafka.reactive.server.ReactiveRoute._
  import KafkaRequestEnvelope._

  import io.circe.parser._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import system.dispatcher

  val route = requestFuture[String, String]("echo")(identity)

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer, Set("echoInbound"))
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withClientId("reactiveService1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  val source: Source[KafkaRequestEnvelope, _] = Consumer.atMostOnceSource(consumerSettings.withClientId("client1"))
    .map { record =>
      decode[KafkaRequestEnvelope](record.value)
    }.filter(_.isRight).map {
      case (Xor.Right(kkReqEnvelope)) => kkReqEnvelope
    }

  val sink: Sink[Future[KafkaResponseEnvelope], _] = Flow[Future[KafkaResponseEnvelope]].map[ProducerRecord[String, String]] { fResp =>
    //TODO: only because this is a test class, I will create a source ad hoc to support Future[ProducerRecord]!!!
    val resp = Await.result(fResp, Duration.Inf)
    new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
  }.to(Producer.plainSink(producerSettings))

  val rsys = ReactiveSystem(source, route, sink)

  def run() = rsys.run()
}