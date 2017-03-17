package org.patricknoir.kafka.reactive.server.streams

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.ProducerMessage.Message
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.kafka._
import akka.stream.scaladsl.{ Flow, Sink, Source }
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
 * This implementation is not using the back-pressure
 * source - router - sink should use mapAsync with maxConcurrency parameter.
 * Created by patrick on 23/07/2016.
 */
object ReactiveKafkaSource {

  def create(requestTopic: String, bootstrapServers: Set[String], clientId: String, groupId: String = "group1", maxConcurrency: Int = 1)(implicit system: ActorSystem, ec: ExecutionContext): Source[KafkaRequestEnvelope, _] =
    Consumer.atMostOnceSource(createConsumerSettings(bootstrapServers, clientId, groupId), Subscriptions.topics(Set(requestTopic)))
      .mapAsync(maxConcurrency) { record => //TODO:  use batching where possible (.groupedWithin())
        Future(decode[KafkaRequestEnvelope](record.value))
      }.filter(_.isRight).map { //TODO: improve with a flow and send the failures to a topic auditing.
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
    flow.to(Producer.commitableSink(producerSettings))
  }

  def createSync(bootstrapServers: Set[String])(implicit system: ActorSystem): Sink[KafkaResponseEnvelope, _] = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    Flow[KafkaResponseEnvelope].map { resp => new ProducerRecord[String, String](resp.replyTo, resp.asJson.noSpaces) }.to(Producer.plainSink(producerSettings))
  }
}
