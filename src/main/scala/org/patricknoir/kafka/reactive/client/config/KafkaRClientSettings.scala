package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.{ ConfigFactory, Config }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRClientSettings(config: Config) {

  val producerSettings = config.as[Map[String, String]]("kafka.client.producer.settings")
  val consumerSettings = config.as[Map[String, String]]("kafka.client.consumer.settings")

  val inboundResponseQueue = config.as[String]("kafka.client.queues.inbound")
  val pollTimeoutDuration: FiniteDuration = config.as[FiniteDuration]("kafka.client.consumer.pollFrequency")

}

object KafkaRClientSettings {
  lazy val default = new KafkaRClientSettings(ConfigFactory.load())

  def apply(config: Config) = new KafkaRClientSettings(config)

  def load(resource: String) = KafkaRClientSettings(ConfigFactory.load(resource))
}