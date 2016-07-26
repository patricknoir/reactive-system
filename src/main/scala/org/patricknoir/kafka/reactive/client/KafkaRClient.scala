package org.patricknoir.kafka.reactive.client

import akka.actor.ActorSystem
import akka.util.Timeout
import cats.data.Xor
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import org.patricknoir.kafka.reactive.client.actors.{ KafkaRClientActor, KafkaConsumerActor, KafkaProducerActor }
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.common.{ ReactiveDeserializer, ReactiveSerializer }
import org.patricknoir.kafka.reactive.common.serializer._
import scala.concurrent.Future
import akka.pattern.ask

/**
 * Created by patrick on 12/07/2016.
 */
trait ReactiveClient {

  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout): Future[Error Xor Out]

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
  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout): Future[Error Xor Out] =
    (kafkaClientService ? KafkaRequest(destination, new String(serialize(payload)), timeout, settings.inboundResponseQueue, implicitly[ReactiveDeserializer[Out]])).mapTo[Error Xor Out]

}
