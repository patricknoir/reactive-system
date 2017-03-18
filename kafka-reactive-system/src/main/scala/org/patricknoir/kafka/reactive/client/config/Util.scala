package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.Config
import scala.collection.JavaConversions._

/** Contains utility methods used internally */
object Util {

  /**
   * Converts a [[com.typesafe.config.Config]] into a [[scala.collection.Map]]
   *
   * @param config configuration object
   * @return representation of a [[com.typesafe.config.Config]] as a [[scala.collection.Map]] instance
   */
  def convertToMap(config: Config): Map[String, String] =
    config.entrySet().map(entry => (entry.getKey, entry.getValue.unwrapped().toString())).toMap

}

