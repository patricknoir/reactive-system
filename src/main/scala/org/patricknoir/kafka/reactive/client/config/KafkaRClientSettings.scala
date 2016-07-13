package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.{ ConfigFactory, Config }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRClientSettings(config: Config) {

  val producerSettings = Map.empty[String, String]
  val consumerSettings = Map.empty[String, String]

  val inboundResponseQueue = "inbound"
  val pollTimeoutDuration: FiniteDuration = 100 millis

}

object KafkaRClientSettings {
  lazy val default = new KafkaRClientSettings(ConfigFactory.load())

  def apply(config: Config) = new KafkaRClientSettings(config)

  def load(resource: String) = KafkaRClientSettings(ConfigFactory.load(resource))
}