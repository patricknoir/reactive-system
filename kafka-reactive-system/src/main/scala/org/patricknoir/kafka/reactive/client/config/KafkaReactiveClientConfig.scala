package org.patricknoir.kafka.reactive.client.config

import akka.actor.ActorSystem
import com.typesafe.config.Config
import akka.kafka.{ ConsumerSettings, ProducerSettings }
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }
import collection.JavaConverters._

/**
 * Created by josee on 29/03/2017.
 */
case class KafkaReactiveClientConfig(
  parallelism:    Int,
  responseTopic:  String,
  consumerConfig: ConsumerSettings[String, String],
  producerConfig: ProducerSettings[String, String]
)

object KafkaReactiveClientConfig {
  def apply(config: Config): KafkaReactiveClientConfig = KafkaReactiveClientConfig(
    parallelism = config.getInt("parallelism"),
    responseTopic = config.getString("response-topic"),
    consumerConfig = ConsumerSettings(config.getConfig("consumer"), new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(config.getStringList("consumer.bootstrap-servers").asScala.mkString(","))
      .withGroupId(config.getString("consumer.group-id")),
    producerConfig = ProducerSettings(config.getConfig("producer"), new StringSerializer, new StringSerializer)
      .withBootstrapServers(config.getStringList("producer.bootstrap-servers").asScala.mkString(","))
  )

  def default()(implicit system: ActorSystem) = apply(system.settings.config.getConfig("reactive.system.client.kafka"))
}