package org.patricknoir.kafka.reactive.server.streams

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.ProducerMessage.Message
import akka.kafka._
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope }

import scala.concurrent.{ ExecutionContext, Future }

object ReactiveKafkaSource extends LazyLogging {

  /**
   * Create a [[Source]] for [[KafkaRequestEnvelope]] messages using a specified
   * concurrency level and a dedicated dispatcher for the deserialization.
   * Note that the dispatcher used for the Kafka consumer is specified by the
   * configuration property: `akka.kafka.consumer.use-dispatcher`
   *
   * @param requestTopic
   * @param bootstrapServers
   * @param clientId
   * @param groupId
   * @param maxConcurrency
   * @param system
   * @param deserializerExecutionContext
   * @return
   */
  def create(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String, maxConcurrency: Int)(implicit system: ActorSystem, deserializerExecutionContext: ExecutionContext): Source[KafkaRequestEnvelope, _] =
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId, true), Subscriptions.topics(Set(requestTopic)))
      .mapAsync(maxConcurrency) { record => //TODO:  use batching where possible (.groupedWithin())
        Future(decode[KafkaRequestEnvelope](record.value))
      }.map { parsedMsg =>
        parsedMsg.left.map(err => logger.error(s"Error while parsing KafkaRequestEnvelope: ${err.getMessage}", err))
        parsedMsg
      }.filter(_.isRight).map {
        case (Right(kkReqEnvelope)) => kkReqEnvelope
        case msg                    => logger.warn(s"Unexpected parsed message received: $msg"); throw new RuntimeException(s"Unexpected parsed message received: $msg")
      }

  /**
   * Read the settings from `application.conf` with fallback to
   * kafkaRS `reference.conf`.
   * @param servers
   * @param clientId
   * @param groupId
   * @param autoCommit
   * @param system
   * @return
   */
  def createConsumerSettings(servers: Set[String], clientId: String, groupId: String, autoCommit: Boolean)(implicit system: ActorSystem) =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(servers.mkString(","))
      .withGroupId(groupId)
      .withClientId(clientId)
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit.toString)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def atLeastOnce(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1")(implicit system: ActorSystem): Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _] =
    Consumer.committableSource(createConsumerSettings(bootstrapServers, clientId, groupId, false), Subscriptions.topics(Set(requestTopic))).map { msg: CommittableMessage[String, String] =>
      (msg, decode[KafkaRequestEnvelope](msg.record.value))
    }.map { msg =>
      msg._2.left.map(err => logger.error(s"Error while parsing KafkaResponseEnvelope: ${err.getMessage}", err))
      msg
    }.filter(_._2.isRight).map {
      case (msg, Right(kkReqEnvelope)) => (msg, kkReqEnvelope)
      case msg                         => logger.warn(s"Unexpected parsed message received: $msg"); throw new RuntimeException(s"Unexpected parsed message received: $msg")
    }
}

object ReactiveKafkaSink {

  def create(bootstrapServers: Set[String], concurrency: Int)(implicit system: ActorSystem, ec: ExecutionContext): Sink[Future[KafkaResponseEnvelope], _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[Future[KafkaResponseEnvelope]].mapAsync[ProducerRecord[String, String]](concurrency) { fResp =>
      fResp.map { resp =>
        new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
      }
    }.filterNot(_.topic == "").to(Producer.plainSink(producerSettings))
  }

  def atLeastOnce(bootstrapServers: Set[String], concurrency: Int)(implicit system: ActorSystem, ec: ExecutionContext): Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    val flow: Flow[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), Message[String, String, ConsumerMessage.Committable], _] = Flow[(CommittableMessage[String, String], Future[KafkaResponseEnvelope])].mapAsync[Message[String, String, ConsumerMessage.Committable]](concurrency) {
      case (msg, fResp) =>
        fResp.map { resp =>
          val record = new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
          ProducerMessage.Message(record, msg.committableOffset)
        }
    }
    flow.map(msg => {
      if (msg.record.topic == "") msg.passThrough.commitScaladsl(); msg
    }).filterNot(_.record.topic == "").to(Producer.commitableSink(producerSettings))
  }

  def createSync(bootstrapServers: Set[String])(implicit system: ActorSystem): Sink[KafkaResponseEnvelope, _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[KafkaResponseEnvelope].map { resp => new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces) }.to(Producer.plainSink(producerSettings))
  }
}
