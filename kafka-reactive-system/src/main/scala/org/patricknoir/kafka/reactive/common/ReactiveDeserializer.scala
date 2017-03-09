package org.patricknoir.kafka.reactive.common

import io.circe.Decoder
import io.circe.parser._

/**
 * Created by patrick on 25/07/2016.
 */
trait ReactiveDeserializer[Payload] {

  def deserialize(input: Array[Byte]): Either[Error, Payload]

}

object ReactiveDeserializer {
  implicit val stringDeserializer = new ReactiveDeserializer[String] {
    override def deserialize(input: Array[Byte]) = Right(new String(input))
  }

  implicit def circeDecoderDeserializer[Out: Decoder] = new ReactiveDeserializer[Out] {
    override def deserialize(input: Array[Byte]) = decode[Out](new String(input)).left.map(err => new Error(err)) //FIXME : use custom errors
  }

  implicit val byteArrayDeserializer = new ReactiveDeserializer[Array[Byte]] {
    override def deserialize(input: Array[Byte]) = Right(input)
  }
}
