package org.patricknoir.kafka.reactive.client.integration

import java.util.Properties

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{ Producer, Consumer }
import akka.kafka.{ ProducerSettings, ConsumerSettings }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import akka.util.Timeout
import cats.data.Xor
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
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import akka.stream.scaladsl._

/**
 * Created by patrick on 16/07/2016.
 */
abstract class BaseIntegrationSpecification extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

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

    base test

  """

  def before() = {
    implicit val materializer = ActorMaterializer()
    val echoService = new KafkaEchoService()
    echoService.run()
  }

  def simpleTest = {
    startUp()
    Thread.sleep(10 * 1000) //10secs
    before()
    Thread.sleep(5 * 1000) //5secs
    implicit val timeout = Timeout(3 seconds)
    val client = new KafkaReactiveClient(KafkaRClientSettings.default)
    Thread.sleep(5 * 1000) //5secs
    val fResponse = client.request[String, String]("kafka:echoInbound/echo", "patrick")

    val Xor.Right(result: String) = Await.result(fResponse, Duration.Inf)

    result must be_==("patrick")
  }
}

class KafkaEchoService(implicit system: ActorSystem, materializer: Materializer) {

  import org.patricknoir.kafka.reactive.server.ReactiveRoute._
  import KafkaRequestEnvelope._

  import io.circe.parser._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import system.dispatcher

  val route = requestFuture[String, String]("echo") { in =>
    println(s"\n\n\nRoute received input: $in\n\n")
    in
  }

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer, Set("echoInbound"))
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  val source: Source[KafkaRequestEnvelope, _] = Consumer.atMostOnceSource(consumerSettings.withClientId("client1"))
    .map { record =>
      println(s"\n\n\nSource decoding: ${record.value}\n\n")
      decode[KafkaRequestEnvelope](record.value)
    }.filter(_.isRight).map { case (Xor.Right(kkReqEnvelope)) => kkReqEnvelope }

  val sink: Sink[Future[KafkaResponseEnvelope], _] = Flow[Future[KafkaResponseEnvelope]].map[ProducerRecord[String, String]] { fResp =>
    //only because this is a test class!!!
    val resp = Await.result(fResp, Duration.Inf)
    println(s"\n\n\nSink replying to: ${resp.replyTo}\n\n")
    new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
  }.to(Producer.plainSink(producerSettings))

  val rsys = ReactiveSystem(source, route, sink)

  def run() = rsys.run()
}