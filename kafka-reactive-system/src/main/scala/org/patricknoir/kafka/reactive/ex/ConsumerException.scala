package org.patricknoir.kafka.reactive.ex

/**
 * Represents failures occurred in the [[org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor]]
 *
 * Thrown by the consumer actor in case of failure and to be handled
 * by the supervisor.
 *
 * TBD Constructor arguments
 */
class ConsumerException(cause: Throwable) extends Exception(cause) {

}
