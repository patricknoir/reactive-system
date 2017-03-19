# Server

@@toc{ depth=3 }

## Overview

The server is where you define the services which will be exposed by your application and that can be
consumed using asynchronous message exchange.
One of the strengths of Reactive System is that streaming data is at its heart (The server is build using Akka Stream) meaning that both requests and responses
can be streamed through the server achieving constant memory usage even for very large requests or responses. Streaming
responses will be backpressured against the kafka queue utilised by the server.


## Define Service using Routing DSL 

A Reactive System Route is in essence a map of services exposed by the server indexed against a service id.

![rroute](images/reactive-route.png)

Here is an example how you can create a route:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #route-dsl-example }

The above code snipped generates a route that declares 3 services:

![rroute-example](images/reactive-route-example.png)

## Reactive Services

In order to create a service using a high level API the `request` object should be used. 
A service should be a simple function that given an `input` of type `In` should return an `output` of type `Out`.
If you want this code to be executed asynchronously then the `request.aSync(serviceId: String)(f: In => Out)` should be used,
otherwise we offer synchronous execution by using `request.sync(serviceId: String)(f: In => Out)`.
A last option is if your function is already returning a future having a signature like the following: `f: In => Future[Out]`, 
in that case you should be using the `request.apply(serviceId: String)(f: In => Future[Out])`.

### Examples

Here is an example how to execute a function asynchronously:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #route-example-make-async }

In this case we will force the `incrementCounter` function to run synchronously to avoid raise conditions:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #route-example-make-sync }

In this last case the `getCounter` function is already returning a `Future[Int]` so we will use `request.apply`:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #route-example-wrap-async }



## Running the server

Once all the services to expose have been defined in the `route: ReactiveRoute` is time to create an instance of `ReactiveSystem` and run it.

The simplest way to create and run a `ReactiveSystem` is the following:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #run-reactive-system }

## Message Delivery Semantics

When creating a server 2 options are available based on the message delivery semantics to be applied:

* At Most Once
* At Least Once

### At Most Once Semantic

A Reactive System which implements `at-most-once` message delivery semantic favours the performance over the consistency.
under this condition a Reactive System servers might be losing messages before they are delivered to the router or before a 
response is sent back to the client.
This strategy should be adopted when performance (especially latency) is a key requirement, the system has to serve a huge 
number of requests/responses mainly read operations which don't cause side effects.
Under these conditions losing a message don't compromise consistency and if a request message get lost is responsibility of
the client to fire another request. An example of adoption of this strategy is the `Query` part of a CQRS/ES architecture.

Here is an example on how to create a `ReactiveSystem` with semantic `at-most-once`:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #run-reactive-system }

### At Least Once Semantic

A Reactive System which implements `at-least-once` message delivery semantic guarantees that all the requests will be processed 
by the services ()defined in the route), but in case of failures the messages can be duplicated and the order is not guaranteed if 
batching is used. Where semantic `exactly-once` is difficult to achieve or it comes with a huge overhead and can compromise the performance,
a similar result can be obtained using `at-least-once` semantic with `Idempotent` services, in practice services capable to detect
duplicated messages and not triggering side affects twice.
This strategy should be adopted when for example implementing `Commands` in a CQRS/ES architecture making sure the services respect
the `Idempotence`.

Here is an example on how to create a `ReactiveSystem` with semantic `at-least-once`:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #reactive-system-at-least-once }
