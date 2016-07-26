package org.patricknoir.kafka.reactive

import cats.data.Xor

/**
 * Created by patrick on 26/07/2016.
 */
package object common {

  object deserializer {
    def deserialize[Out: ReactiveDeserializer](in: Array[Byte]): Xor[Error, Out] = implicitly[ReactiveDeserializer[Out]].deserialize(in)
  }

  object serializer {
    def serialize[In: ReactiveSerializer](in: In): Array[Byte] = implicitly[ReactiveSerializer[In]].serialize(in)
  }

}
