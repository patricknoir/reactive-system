package org.patricknoir.reactive.http

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server._
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.ReactiveClient

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by patrick on 13/08/2016.
 */
object KafkaHttpRoute {

  /**
   * Note: There is also an implicit conversion from Route to
   *
   * Flow[HttpRequest, HttpResponse, Unit]
   *
   * defined in the RouteResult companion, which relies on Route.handlerFlow.
   *
   * That way I can transform the client in a Flow and wrap into a Akka HTTP Route!!!
   *
   * @param client
   * @param kafkaHost
   * @return
   */
  def create(client: ReactiveClient, kafkaHost: String)(implicit ec: ExecutionContext, timeout: Timeout): Route = (context: RequestContext) => {

    val pathString = context.request.getUri().path()

    val response: Future[String] = client.request[String, String](s"$kafkaHost/$pathString", extractPayload(context.request))

    response.map(createRouteResult)
  }

  private def extractPayload(request: HttpRequest): String = ???

  private def createRouteResult(result: String): RouteResult = ???

}
