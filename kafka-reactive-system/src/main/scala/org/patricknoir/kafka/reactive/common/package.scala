package org.patricknoir.kafka.reactive

import io.circe.Decoder
import io.circe.generic.auto._

import scala.util.Try

/**
 * Provides objects to help with serialization and deserialization of objects
 *
 * Used by the [[org.patricknoir.kafka.reactive.client.ReactiveClient]] in order to serialize/deserialize request and response
 * but also utlized by the [[org.patricknoir.kafka.reactive.server.ReactiveSystem]] in order to serialize/deserialize input and output elements
 * of a [[org.patricknoir.kafka.reactive.server.ReactiveService]].
 *
 */
package object common {

  /** Used to deserialize json strings into types members of [[org.patricknoir.kafka.reactive.common.ReactiveDeserializer]] */
  object deserializer {
    /**
     * Try to deserialize an input json string into an Out member of [[ReactiveDeserializer]]
     *
     * @param in json string
     * @tparam Out a type which can be serialize by being a member of the type class: [[ReactiveDeserializer]]
     * @return a [[scala.util.Left]] of [[java.lang.Throwable]] if fails, [[scala.util.Right]] of an [[Out]] in case of success
     */
    def deserialize[Out: ReactiveDeserializer](in: String): Either[Throwable, Out] = implicitly[ReactiveDeserializer[Out]].deserialize(in.getBytes)
  }

  /** Used to serialize types In members of [[org.patricknoir.kafka.reactive.common.ReactiveSerializer]] into json strings */
  object serializer {
    /**
     * Try to serialize an input In member of [[ReactiveSerializer]] into a json string representation
     *
     * @param in entity to be serialized
     * @tparam In input type member of the typeclass [[ReactiveSerializer]]
     * @return a [[scala.util.Left]] of [[java.lang.Throwable]] if fails, [[scala.util.Right]] of an [[In]] in case of success
     */
    def serialize[In: ReactiveSerializer](in: In): String = new String(implicitly[ReactiveSerializer[In]].serialize(in))
  }

  case class Destination(medium: String, topic: String, serviceId: String) {
    override def toString: String = {
      s"$medium:$topic"
    }
  }
  object Destination {
    def unapply(destination: String): Option[(String, String, String)] = Try {
      val mediumAndTopic = destination.split(":")
      val medium = mediumAndTopic(0)
      val topicAndRoutes = mediumAndTopic(1).split("/")
      val topic = topicAndRoutes(0)
      val route = topicAndRoutes.drop(1).mkString("/")
      (medium, topic, route)
    }.toOption
  }

  case class KafkaRequestEnvelope(correlationId: String, destination: Destination, payload: String, replyTo: String)
  object KafkaRequestEnvelope {
    implicit val kafkaRequestEnvelopeDecoder = Decoder.instance[KafkaRequestEnvelope] { c =>
      for {
        correlationId <- c.downField("correlationId").as[String]
        destination <- c.downField("destination").as[Destination]
        payload <- c.downField("payload").as[String]
        replyTo <- c.downField("replyTo").as[String]
      } yield KafkaRequestEnvelope(correlationId, destination, payload, replyTo)
    }
  }

  object KafkaResponseStatusCode {
    val Success = 200
    val NotFound = 404
    val BadRequest = 300
    val InternalServerError = 500
  }
  case class KafkaResponseEnvelope(correlationId: String, replyTo: String, response: String, statusCode: Int)
  object KafkaResponseEnvelope {
    implicit val respEnvelopeDecoder = Decoder.instance[KafkaResponseEnvelope] { c =>
      for {
        correlationId <- c.downField("correlationId").as[String]
        replyTo <- c.downField("replyTo").as[String]
        response <- c.downField("response").as[String]
        statusCode <- c.downField("statusCode").as[Int]
      } yield KafkaResponseEnvelope(correlationId, replyTo, response, statusCode)
    }
  }
}
