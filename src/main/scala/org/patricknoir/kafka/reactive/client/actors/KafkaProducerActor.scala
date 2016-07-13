package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ Props, ActorLogging, Actor }
import akka.event.LoggingReceive
import org.apache.kafka.clients.producer.{ ProducerRecord, KafkaProducer }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.JavaConversions._

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaProducerActor(producerSettings: Map[String, String]) extends Actor with ActorLogging {

  val producer = new KafkaProducer[String, String](producerSettings)

  def receive = LoggingReceive {
    case envelope: KafkaRequestEnvelope =>
      val record = new ProducerRecord[String, String](envelope.destination, envelope.asJson.noSpaces)
      producer.send(record)
      ()
  }

  override def postStop() = {
    producer.close()
  }

}

object KafkaProducerActor {
  case class KafkaRequestEnvelope(correlationId: String, destination: String, payload: String, replyTo: String)
  def props(producerSettings: Map[String, String]) = Props(new KafkaProducerActor(producerSettings))
}
