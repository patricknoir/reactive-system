package org.patricknoir.kafka.reactive

import cats.data.Xor
import io.circe.Error

/**
 * Created by patrick on 26/07/2016.
 */
package object common {

  object deserializer {
    def deserialize[Out: ReactiveDeserializer](in: String): Xor[Error, Out] = implicitly[ReactiveDeserializer[Out]].deserialize(in.getBytes)
  }

  object serializer {
    def serialize[In: ReactiveSerializer](in: In): String = new String(implicitly[ReactiveSerializer[In]].serialize(in))
  }

}
