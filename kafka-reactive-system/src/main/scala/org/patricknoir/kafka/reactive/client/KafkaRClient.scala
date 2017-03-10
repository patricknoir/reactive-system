package org.patricknoir.kafka.reactive.client

import akka.actor.ActorSystem
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.{ Destination, KafkaRequest }
import org.patricknoir.kafka.reactive.client.actors.{ KafkaConsumerActor, KafkaProducerActor, KafkaRClientActor }
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.common.{ ReactiveDeserializer, ReactiveSerializer }
import org.patricknoir.kafka.reactive.common.serializer._

import scala.concurrent.Future
import akka.pattern.ask

import scala.reflect.ClassTag

/**
 * Created by patrick on 12/07/2016.
 */
trait ReactiveClient {

  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout, ct: ClassTag[Out]): Future[Out]

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
  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout, ct: ClassTag[Out]): Future[Out] = {
    destination match {
      case Destination(medium, topic, route) =>
        (kafkaClientService ? KafkaRequest(
          Destination(medium, topic, route),
          serialize(payload),
          timeout,
          settings.inboundResponseQueue,
          implicitly[ReactiveDeserializer[Out]]
        )).mapTo[Out]
      case _ => Future.failed[Out](new RuntimeException("unknown destination"))
    }
  }
}
