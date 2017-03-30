package org.patricknoir.kafka.reactive.server.streams

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.ProducerMessage.Message
import akka.kafka._
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * This implementation is not using the back-pressure
 * source - router - sink should use mapAsync with maxConcurrency parameter.
 * Created by patrick on 23/07/2016.
 */
object ReactiveKafkaSource {

  def create(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1", maxConcurrency: Int = 1)(implicit system: ActorSystem, ec: ExecutionContext): Source[KafkaRequestEnvelope, _] =
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(requestTopic)))
      .mapAsync(maxConcurrency) { record => //TODO:  use batching where possible (.groupedWithin())
        Future(decode[KafkaRequestEnvelope](record.value))
      }.filter(_.isRight).map {
        //TODO: improve with a flow and send the failures to a topic auditing.
        case (Right(kkReqEnvelope)) => kkReqEnvelope
      }

  def createConsumerSettings(servers: Set[String], clientId: String, groupId: String)(implicit system: ActorSystem) =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(servers.mkString(","))
      .withGroupId(groupId)
      .withClientId(clientId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def atLeastOnce(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1")(implicit system: ActorSystem): Source[(CommittableMessage[String, String], KafkaRequestEnvelope), _] =
    Consumer.committableSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(requestTopic))).map { msg: CommittableMessage[String, String] =>
      (msg, decode[KafkaRequestEnvelope](msg.record.value))
    }.filter(_._2.isRight).map {
      case (msg, Right(kkReqEnvelope)) => (msg, kkReqEnvelope)
    }
}

object ReactiveKafkaSink {

  def create(bootstrapServers: Set[String])(implicit system: ActorSystem, ec: ExecutionContext): Sink[Future[KafkaResponseEnvelope], _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[Future[KafkaResponseEnvelope]].mapAsync[ProducerRecord[String, String]](1) { fResp =>
      fResp.map { resp =>
        new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces)
      }
    }.filterNot(_.topic == "").to(Producer.plainSink(producerSettings))
  }

  def atLeastOnce(bootstrapServers: Set[String])(implicit system: ActorSystem, ec: ExecutionContext): Sink[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    val flow: Flow[(CommittableMessage[String, String], Future[KafkaResponseEnvelope]), Message[String, String, ConsumerMessage.Committable], _] = Flow[(CommittableMessage[String, String], Future[KafkaResponseEnvelope])].mapAsync[Message[String, String, ConsumerMessage.Committable]](1) {
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
