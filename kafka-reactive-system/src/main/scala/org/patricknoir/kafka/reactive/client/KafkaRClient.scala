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
 * Client interface to perform requests to a Reactive System Server
 *
 * Used to make requests to a service exposed by Reactive System Server
 *
 * ==Overview==
 *
 * {{{
 *   val client: ReactiveClient = ...
 *
 *   val futureSize: Future[Int] = client.request[String, Int]("length")("hello world")
 *
 * }}}
 *
 */
trait ReactiveClient {

  /**
   * Used to perform requests to a Service exposed by a ReactiveSystem server instance.
   * @param destination string representing a url made with format: 'protocol:topicName/serviceId'
   * @param payload input parameter of the remote service we are invoking
   * @param timeout how long the client has to wait before to expire the request and fail with a timeout error
   * @param ct used by the compiler in order to deserialize the response
   * @tparam In payload input type member of [[org.patricknoir.kafka.reactive.common.ReactiveSerializer]]
   * @tparam Out result type member of [[org.patricknoir.kafka.reactive.common.ReactiveDeserializer]]
   * @return a [[scala.concurrent.Future]] of an [[Out]] in case of success, otherwise a failed [[scala.concurrent.Future]]
   */
  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout, ct: ClassTag[Out]): Future[Out]

}

/**
 * Concrete implementation of a [[ReactiveClient]] using Kafka broker.
 * @param settings configuration for connecting to the kafka broker
 * @param system actor system to be used by this instance
 */
class KafkaReactiveClient(settings: KafkaRClientSettings)(implicit system: ActorSystem) extends ReactiveClient {

  val producerProps = KafkaProducerActor.props(settings.producerSettings)
  val consumerProps = KafkaConsumerActor.props(settings.consumerSettings, settings.inboundResponseQueue, settings.pollTimeoutDuration)

  val kafkaClientService = system.actorOf(KafkaRClientActor.props(producerProps, consumerProps), "clientService")

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
