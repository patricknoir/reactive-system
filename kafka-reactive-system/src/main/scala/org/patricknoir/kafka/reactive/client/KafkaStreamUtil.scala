package org.patricknoir.kafka.reactive.client

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.kafka.{ ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.scaladsl._
import io.circe.parser.decode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope }
import io.circe.generic.auto._

import scala.concurrent.{ ExecutionContext, Future }

object ReactiveKafkaStreamSource {

  def atMostOnce(responseTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1", maxConcurrency: Int)(implicit system: ActorSystem, ec: ExecutionContext): Source[KafkaResponseEnvelope, _] = {
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(responseTopic)))
      .mapAsync(maxConcurrency) { record => //TODO:  use batching where possible (.groupedWithin())
        Future(decode[KafkaResponseEnvelope](record.value))
      }.filter(_.isRight).map { //TODO: improve with a flow and send the failures to a topic auditing.
        case (Right(kkRespEnvelope)) => kkRespEnvelope
      }
  }

  def createConsumerSettings(servers: Set[String], clientId: String, groupId: String)(implicit system: ActorSystem) =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(servers.mkString(","))
      .withGroupId(groupId)
      .withClientId(clientId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "5000")
}
