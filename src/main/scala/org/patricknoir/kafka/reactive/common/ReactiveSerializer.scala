package org.patricknoir.kafka.reactive.common

import io.circe.Encoder
import io.circe.syntax._

/**
 * Created by patrick on 25/07/2016.
 */
trait ReactiveSerializer[Payload] {

  def serialize(payload: Payload): Array[Byte]

}

object ReactiveSerializer {
  implicit val stringSerializer = new ReactiveSerializer[String] {
    override def serialize(payload: String) = payload.getBytes
  }

  implicit def circeEncoderSerializer[In: Encoder] = new ReactiveSerializer[In] {
    override def serialize(payload: In) = payload.asJson.noSpaces.getBytes
  }

  implicit val byteArraySerializer = new ReactiveSerializer[Array[Byte]] {
    override def serialize(payload: Array[Byte]) = payload
  }
}