package org.patricknoir.kafka.reactive.client

import akka.actor.ActorSystem
import akka.util.Timeout
import cats.data.Xor
import io.circe.{ Decoder, Encoder }
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import org.patricknoir.kafka.reactive.client.actors.{ KafkaRClientActor, KafkaConsumerActor, KafkaProducerActor }
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import io.circe.syntax._
import scala.concurrent.Future
import akka.pattern.ask

/**
 * Created by patrick on 12/07/2016.
 */
trait ReactiveClient {

  def request[In: Encoder, Out: Decoder](destination: String, payload: In)(implicit timeout: Timeout): Future[Error Xor Out]

}

class KafkaReactiveClient(settings: KafkaRClientSettings)(implicit system: ActorSystem) extends ReactiveClient {

  val producerProps = KafkaProducerActor.props(settings.producerSettings)
  val consumerProps = KafkaConsumerActor.props(settings.consumerSettings, settings.inboundResponseQueue, settings.pollTimeoutDuration)

  val kafkaClientService = system.actorOf(KafkaRClientActor.props(producerProps, consumerProps), "clientService")

  /**
   *
   * @param destination format is: kafka:destinationTopic/serviceId
   * @param payload
   * @param timeout
   * @tparam In
   * @tparam Out
   * @return
   */
  //TODO: I'm messing with Error vs Throwable => replace Error with Throwable!
  def request[In: Encoder, Out: Decoder](destination: String, payload: In)(implicit timeout: Timeout): Future[Error Xor Out] =
    (kafkaClientService ? KafkaRequest(destination, payload.asJson.noSpaces, timeout, settings.inboundResponseQueue, implicitly[Decoder[Out]])).mapTo[Error Xor Out]

}
