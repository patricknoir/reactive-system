package org.patricknoir.kafka.reactive.client2

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

  def atMostOnce(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1", maxConcurrency: Int = 1)(implicit system: ActorSystem, ec: ExecutionContext): Source[KafkaResponseEnvelope, _] = {
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(requestTopic)))
      .mapAsync(maxConcurrency) { record => //TODO:  use batching where possible (.groupedWithin())
        Future(decode[KafkaResponseEnvelope](record.value))
      }.filter(_.isRight).map { //TODO: improve with a flow and send the failures to a topic auditing.
        case (Right(kkReqEnvelope)) => kkReqEnvelope
      }
  }

  def createConsumerSettings(servers: Set[String], clientId: String, groupId: String)(implicit system: ActorSystem) =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(servers.mkString(","))
      .withGroupId(groupId)
      .withClientId(clientId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
}

object ReactiveKafkaStreamSink {

  import io.circe.syntax._

  def atMostOnce(bootstrapServers: Set[String], maxConcurrency: Int = 1)(implicit system: ActorSystem, ec: ExecutionContext): Sink[Future[KafkaRequestEnvelope], _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[Future[KafkaRequestEnvelope]].mapAsync[ProducerRecord[String, String]](maxConcurrency) { fReq =>
      fReq.map { req =>
        new ProducerRecord[String, String](req.replyTo, req.asJson.noSpaces)
      }
    }.filterNot(_.topic == "").to(Producer.plainSink(producerSettings))
  }
}
