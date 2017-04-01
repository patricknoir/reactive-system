package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.convert.ImplicitConversions._

class ClientConfig(
  val concurrency:      Int,
  val bootstrapServers: Set[String],
  val responseTopic:    String,
  val clientId:         String,
  val groupId:          String
)

object ClientConfig {
  lazy val default = ClientConfig(ConfigFactory.load())

  def apply(config: Config) = new ClientConfig(
    concurrency = config.getInt("reactive.system.client.concurrency"),
    bootstrapServers = config.getStringList("reactive.system.client.servers").toSet,
    responseTopic = config.getString("reactive.system.client.response.topic"),
    clientId = config.getString("reactive.system.client.clientId"),
    groupId = config.getString("reactive.system.client.groupId")
  )
}