package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.convert.ImplicitConversions._

/**
  * Client configuration
  * @param concurrency level of parallelism used by the client when serialising the requests.
  * @param bootstrapServers set of host addresses in the shape `host:port` used by the publisher and consumer.
  * @param responseTopic topic that should be used to listen for responses.
  * @param clientId kafka clientId to be used.
  * @param groupId kafka groupId to be used.
  */
class ClientConfig(
  val concurrency:      Int,
  val bootstrapServers: Set[String],
  val responseTopic:    String,
  val clientId:         String,
  val groupId:          String
)

object ClientConfig {
  /**
    * Default ClientConfig which reads the configuration
    * from the `application.conf` in the current classpath.
    */
  lazy val default = ClientConfig(ConfigFactory.load())

  /**
    * Creates a [[ClientConfig]] from a [[Config]] instance.
    * @param config typesafe config instance to be used to build this ClientConfig.
    * @return a valid ClientConfig instance.
    */
  def apply(config: Config) = new ClientConfig(
    concurrency = config.getInt("reactive.system.client.concurrency"),
    bootstrapServers = config.getStringList("reactive.system.client.servers").toSet,
    responseTopic = config.getString("reactive.system.client.response.topic"),
    clientId = config.getString("reactive.system.client.clientId"),
    groupId = config.getString("reactive.system.client.groupId")
  )
}