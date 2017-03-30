package org.patricknoir.kafka.reactive.client.config

/**
 * Created by josee on 29/03/2017.
 */
case class ReactiveClientStreamConfig(
  concurrency:      Int,
  bootstrapServers: Set[String],
  responseTopic:    String,
  consumerClientId: String,
  consumerGroupId:  String
)

object ReactiveClientStreamConfig {
  lazy val default = ReactiveClientStreamConfig(
    concurrency = 8,
    bootstrapServers = Set("localhost:9092"),
    responseTopic = "responses",
    consumerClientId = "testClient1",
    consumerGroupId = "reactiveSystem"
  )
}