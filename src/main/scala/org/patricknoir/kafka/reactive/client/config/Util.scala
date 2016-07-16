package org.patricknoir.kafka.reactive.client.config

import com.typesafe.config.Config
import scala.collection.JavaConversions._

object Util {

  def convertToMap(config: Config): Map[String, String] =
    config.entrySet().map(entry => (entry.getKey, entry.getValue.unwrapped().toString())).toMap

}

