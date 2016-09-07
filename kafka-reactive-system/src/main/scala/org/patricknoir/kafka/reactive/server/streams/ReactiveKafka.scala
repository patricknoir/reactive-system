package org.patricknoir.kafka.reactive.server.streams

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.kafka.{ ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import cats.data.Xor
import io.circe.parser._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import io.circe.generic.auto._
import io.circe.syntax._

/**
 * Created by patrick on 23/07/2016.
 */
object ReactiveKafkaSource {

  def create(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1")(implicit system: ActorSystem): Source[KafkaRequestEnvelope, _] =
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(requestTopic)))
      .map { record =>
        decode[KafkaRequestEnvelope](record.value)
      }.filter(_.isRight).map {
        case (Xor.Right(kkReqEnvelope)) => kkReqEnvelope
      }

  private def createConsumerSettings(servers: Set[String], clientId: String, groupId: String)(implicit system: ActorSystem) =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(servers.mkString(","))
      .withGroupId(groupId)
      .withClientId(clientId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

}

object ReactiveKafkaSink {
  //  def create(bootstrapServers: Set[String])(implicit system: ActorSystem): Sink[Future[KafkaResponseEnvelope], _] = {
  //    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
  //      .withBootstrapServers(bootstrapServers.mkString(","))
  //
  //    Flow[Future[KafkaResponseEnvelope]].map[ProducerRecord[String, String]] { fResp =>
  //      //FIXME: I will create a source ad hoc to support Future[ProducerRecord], this is just to test the functionality
  //      //TODO: replace this with KafkaProducerActor with consumerSettings.properties and map over the future in order to send the message
  //      val resp = Await.result(fResp, Duration.Inf)
  //      new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
  //    }.to(Producer.plainSink(producerSettings))
  //  }

  def create(bootstrapServers: Set[String])(implicit system: ActorSystem, ec: ExecutionContext): Sink[Future[KafkaResponseEnvelope], _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[Future[KafkaResponseEnvelope]].mapAsync[ProducerRecord[String, String]](1) { fResp =>
      fResp.map { resp =>
        new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
      }
    }.to(Producer.plainSink(producerSettings))
  }
}
