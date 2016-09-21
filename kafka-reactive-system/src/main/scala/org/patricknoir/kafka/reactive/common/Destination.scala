package org.patricknoir.kafka.reactive.common

import java.net.URL

import scala.util.Try

/**
 * Created by michal.losiewicz on 21/09/16.
 */
case class Destination(medium: String, topic: String, serviceId: String)
object Destination {
  def unapply(destination: String): Option[(String, String, String)] = Try {
    val mediumAndTopic = destination.split(":")
    val medium = mediumAndTopic(0)
    val topicAndRoutes = mediumAndTopic(1).split("/")
    val topic = topicAndRoutes(0)
    val route = topicAndRoutes.drop(1).mkString("/")
    (medium, topic, route)
  }.toOption

  implicit def fromURL(url: URL): Destination = {
    Destination(url.getProtocol, url.getHost, if(url.getPath.startsWith("/"))url.getPath.drop(1) else url.getPath)
  }
}