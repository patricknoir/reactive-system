package org.patricknoir.kafka.reactive.client

import org.patricknoir.kafka.reactive.client.Committable.CommittableImpl
import org.specs2.Specification

/**
 * Created by patrick on 23/03/2017.
 */
class CommittableSpec extends Specification {

  def is = s2"""
                committable from different topics   $committableFromDifferentTopics
    """

  def committableFromDifferentTopics = {
    val c1: Committable[Int] = createCommit(1, 2, "topic1")
    val c2: Committable[String] = createCommit("hello", 3, "topic2")

    val c3: Committable[(Int, String)] = for {
      v1 <- c1
      v2 <- c2
    } yield (c1, c2)

    (c3.value._1 == c1.value) && (c3.value._2 == c2.value) &&
      (c3.optionOffset == Some(Map(
        ("topic1", 0) -> 2,
        ("topic2", 0) -> 3
      )))
  }

  private def createCommit[A](value: A, offset: Int, topic: String, partition: Int = 0): Committable[A] =
    CommittableImpl(value, Some(Offset(Map((topic, partition) -> offset))), None)

}
