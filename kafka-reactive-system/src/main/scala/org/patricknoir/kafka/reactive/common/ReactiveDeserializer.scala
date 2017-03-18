package org.patricknoir.kafka.reactive.common

import io.circe.Decoder
import io.circe.parser._

/**
 * Typeclass for generic types which can be decoded from an `Array[Byte]` representation
 *
 * @tparam Payload type which instances can be derived from an `Array[Byte]` representation
 */
trait ReactiveDeserializer[Payload] {

  /**
   * Try to transform an `Array[Byte]` into a [[Payload]]
   *
   * @param input an `Array[Byte]` representing an instance of [[Payload]]
   * @return a [[scala.util.Right]] of [[Payload]] in case of success, [[scala.util.Left]] of [[Throwable]] for failures
   */
  def deserialize(input: Array[Byte]): Either[Throwable, Payload]

}

/** Provides implementation of base typeclass memebers for [[org.patricknoir.kafka.reactive.common.ReactiveDeserializer]] */
object ReactiveDeserializer {
  /** Deserializer for base type [[java.lang.String]] */
  implicit val stringDeserializer = new ReactiveDeserializer[String] {
    override def deserialize(input: Array[Byte]) = Right(new String(input))
  }

  /**
   * Deserializer for all circe decoder type members
   *
   * If a type [[Out]] is member of [[Decoder]] then is also a valid [[ReactiveDeserializer]] member
   *
   */
  implicit def circeDecoderDeserializer[Out: Decoder] = new ReactiveDeserializer[Out] {
    override def deserialize(input: Array[Byte]) = decode[Out](new String(input)).left.map(err => new RuntimeException(err))
  }

  /** Deserializer for base type `Array[Byte]` */
  implicit val byteArrayDeserializer = new ReactiveDeserializer[Array[Byte]] {
    override def deserialize(input: Array[Byte]) = Right(input)
  }
}
