Reactive Kafka Service
======================

Current Version: **0.2.0-SNAPSHOT**

Introduction
------------
This project aims to provide a new paradigm to implement Reactive Service which are message driven and purely reactive.
This library will enable you to declare server side services exposed by your application using a DSL similar to Akka HTTP
using Kafka Topics rather than http requests/responses.
This project also provides a client library to consume those services which will hide the publisher/consumer by simple function
invocations that returns Future[_] making easy to compose different requests together.

To Fix
------

1. ~~Replace io.circe.Encoder[A]/Decoder[A] typeclasses from the API and replace by a custom typeclass for kafka serialization/deserialization. We will offer implicit converters from/to circe Decoders/Encoders~~
2. ~~Generalise the API from Kafka such as it can be implemented with any messaging system~~
3. FIXME: I have been messing with (Error Xor A) (Error = java.lang.Error) => this should be replaced with Throwable or custom Error type
4. ~~Replace ReactiveRoute.request* DSL methods with Magnet Pattern like in Akka HTTP~~
5. ReactiveSystem implementation is too naive and the only purpose is to show the API usage
6. Implement Semantics At Least Once for Command/Event pattern
7. Implement a ReactiveKafkaSink that accepts Future[KafkaEnvelopeResponse] and is non-blocking.

Import the project
------------------
If you want to use kafka reactive services you need to import this library:

Scala 2.11.x :

```scala
"org.patricknoir.kafka" %% "kafka-reactive-service" % "0.2.0-SNAPSHOT"
```

Depends on kafka-reactive-service from github:

```scala

object V {
  val depProject = "master"
  // Other library versions
}

object Projects {
  lazy val depProject = RootProject(uri("git://github.com/me/dep-project.git#%s".format(V.depProject)))
}

// Library dependencies
lazy val myProject = Project("my-project", file("."))
.settings(myProjectSettings: _*)
.dependsOn(Projects.depProject)
.settings(
  libraryDependencies ++= Seq(...)
  ...
)


```

Reactive System
---------------

### Introduction
A Reactive System is an abstraction of a server which offers different services using message exchange pattern.

The basic idea is that a Reactive System is very similar to a server which exposes services using web-services through RESTful
interfaces, however web-services have most of the time a point-to-point communication which makes composition very hard as the
components are strongly coupled. 
Even if this problem can be mitigated using proxies, load balancing; with Reactive Systems I take
a totally different approach, it still offers the ability to expose Request/Response services similar to the web-service pattern,
however all the communications are happening trough message exchange.

A Reactive System has an inbound queue for the requests addressed to the system. Attached to the inbound queue is the key component of a Reactive System:
the *Reactive Route*.

### Reactive Route

The Reactive Route consumes Reactive Requests from an Akka Streams Source and dispatch the messages to the relative service. 
When you build a reactive system the first thing to define is the Route:

```scala

import ReactiveRoute._

val route: ReactiveRoute = request.aSync("echo") { (in: String) => 
    s"echoing: $in" 
  } ~
  request.aSync[String, Int]("size") { in =>
    in.length
  } ~
  request.sync[String, String]("reverse") { in => in.reverse }

```

The above router configuration will be translated into the below:

```

                                    ______________________________________________________________________________
                                   |                                    Reactive System                           |
                                   |                                                                              |
                                   |                                                                              |
                                   |                                        /---->{Service: echo}                 |
                                   |                                        |                                     |
                                   |             __________                 |                                     |
                                   |            |          |----------------'                                     |
                           ________|______      | Reactive |                                                      |
----[Request Message]---->| Inbound Queue |---->|  Route   |------------------------>{Service: size}              |
                          '--------|------'     |          |                                                      |
                                   |            '----------'                                                      |
                                   |                    |                                                         |
                                   |                    \--------------->{Service: reverse}                       |
                                   |                                                                              |
                                   |                                                                              |
                                   '------------------------------------------------------------------------------'                                   
```

#### Reactive Route API
There is a simple API which allows to create simple Reactive Routes. The key object for the API is the entity request defined as per below:

```scala

object request {
    def apply[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => Future[Error Xor Out]): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(f))
    def sync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => (Error Xor Out)): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future.successful(f(in))))
    def aSync[In: ReactiveDeserializer, Out: ReactiveSerializer](id: String)(f: In => (Error Xor Out))(implicit ec: ExecutionContext): ReactiveRoute =
      ReactiveRoute().add(ReactiveService[In, Out](id)(in => Future(f(in))))
  }
```

...

### Reactive System

### Service Discovery

### Service URL

```
URL : kafka://localhost:9092/inboundTopic/serviceName
```

...

### Example

...

```scala

import org.patricknoir.kafka.reactive.server.ReactiveRoute._

  implicit val system: ActorSystem = ...
  import system.dispatcher

  val source: Source[KafkaRequestEnvelope, _] = ReactiveKafkaSource.create("echoInbound", Set("localhost:9092"), "client1", "group1")

  val route = request.sync[String, String]("echo") { in =>
      "echoing: " + in
    } ~ request.aSync[String, Int]("length") { in =>
      in.length
    } ~ request[String, Int]("parseInt") { in => Future {
        Xor.fromTry(in.toInt)
      }
    }

  val sink: Sink[Future[KafkaResponseEnvelope], _] = ReactiveKafkaSink.create(Set("localhost:9092"))

  val reactiveSys = ReactiveSystem(source, route, sink)

  reactiveSys.run()

```

Consume a Reactive Service
--------------------------

```scala
implicit system: ActorSystem = ...
import system.dispatcher
implicit val timeout = Timeout(3 seconds)
val client = new KafkaReactiveClient(KafkaRClientSettings.default)
val fResponse = client.request[String, String]("kafka:echoInbound/echo", "patrick")
val Xor.Right(result: String) = Await.result(fResponse, Duration.Inf)

println(result)
```