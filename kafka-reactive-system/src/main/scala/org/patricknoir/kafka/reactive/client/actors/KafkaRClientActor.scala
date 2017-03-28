package org.patricknoir.kafka.reactive.client.actors

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.{ KafkaMessage, KafkaRequest }
import org.patricknoir.kafka.reactive.common.ReactiveDeserializer
import org.patricknoir.kafka.reactive.ex.{ ConsumerException, ProducerException }

import scala.concurrent.duration._
import scala.util.{ Success, Try }

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaRClientActor(producerProps: Props, consumerProps: Props) extends Actor with ActorLogging {

  //TODO: add supervisor strategy to handle KafkaProducerException and KafkaConsumerException in order to restart
  // kafka consumer and producer
  override val supervisorStrategy = OneForOneStrategy(5, 60 seconds) {
    case pe: ProducerException =>
      log.error(pe, "producer error")
      Restart
    case ce: ConsumerException =>
      log.error(ce, "consumer error")
      Restart

  }

  val producer = context.actorOf(producerProps, "producer")
  val consumer = context.actorOf(consumerProps, "consumer")

  def createRequestActor(): ActorRef = context.actorOf(KafkaRRequestActor.props(producer), s"request-${UUID.randomUUID()}")

  def receive = LoggingReceive {
    case request: KafkaRequest => createRequestActor() forward request
    case message: KafkaMessage => createRequestActor() forward message
  }

}

object KafkaRClientActor {

  case class Destination(medium: String, topic: String, serviceId: String)
  object Destination {
    def unapply(destination: String): Option[(String, String, String)] =
      fromString(destination).map(d => (d.medium, d.topic, d.serviceId)).toOption

    def fromString(destinationString: String): Try[Destination] = Try {
      val mediumAndTopic = destinationString.split(":")
      val medium = mediumAndTopic(0)
      val topicAndRoutes = mediumAndTopic(1).split("/")
      val topic = topicAndRoutes(0)
      val route = topicAndRoutes.drop(1).mkString("/")
      Destination(medium, topic, route)
    }
  }

  case class KafkaRequest(destination: Destination, payload: String, timeout: Timeout, replyTo: String, decoder: ReactiveDeserializer[_])

  case class KafkaMessage(destination: Destination, payload: String, confirmSend: Boolean)

  def props(producerProps: Props, consumerProps: Props) = Props(new KafkaRClientActor(producerProps, consumerProps))
}
