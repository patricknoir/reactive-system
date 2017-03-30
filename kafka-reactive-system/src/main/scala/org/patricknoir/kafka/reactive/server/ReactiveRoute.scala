package org.patricknoir.kafka.reactive.server

/**
 * Used to define the Services exposed by a [[ReactiveSystem]] server instance
 *
 * @param services services exposed by this server instance indexed by service id
 */
case class ReactiveRoute(services: Map[String, ReactiveService[_, _]] = Map.empty[String, ReactiveService[_, _]]) {
  def add[In, Out](reactiveService: ReactiveService[In, Out]): ReactiveRoute =
    this.copy(services = services + (reactiveService.id -> reactiveService))

  /**
   * Used to merge multpile routes
   * @param reactiveRoute
   * @return a new route which is the merge of the current instance with the passed reactiveRoute parameter
   */
  def ~(reactiveRoute: ReactiveRoute) = ReactiveRoute(this.services ++ reactiveRoute.services)

}

