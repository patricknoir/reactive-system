Introduction to: Reactive System
================================

A reactive system is a server which exposes services using message exchange pattern rather than synchronos IO communications (for example HTTP).
Using message exchange rather than synchronous communication helps to build more decoupled systems, which gives us several advatages like:

1. Isolation: if a reactive system fails it doesn't affect its client.
2. Location transparency: reactive systems they don't communicate directly but through messages sent to 
   an address (topic), that way is not necessary to know the location of the reactive system exposing the
   service as long we can send a message to the topic address.
3. Elasticity: reactive services can implement scaling strategy on the way they consume the messages from a
   topic allowing to add (scale out) and remove (scale down) more reactive systems depending on the current
   demand.
4. DOS Immune: ...


Reactive System
---------------

In order to build a reactive system you need 3 elements:

1. A source of ReactiveRequest messages
2. A router able to dispatch ReactiveRequest messages to the associated ReactiveService
3. A sink of ReactiveResponse messages which will send the response to the right destination

```
    _____________________________________________
   |               REACTIVE SYSTEM               |
   |                                             |
   |      ________      _______      ______      |
   |     |        |    |       |    |      |     |
   |     | Source | ~> | Route | ~> | Sink |     |
   |     |________|    |_______|    |______|     |
   |                                             |
   |_____________________________________________|
   
```

*Scala API:*

```scala

import org.patricknoir.kafka.reactive.server.dsl._

implicit val system: ActorSystem = ...
import system.dispatcher

val source: Source[KafkaRequestEnvelope, _] = ...

val route: ReactiveRoute = ...

val sink: Sink[Future[kafkaResponseEnvelope], _] = ...

val reactiveSys: ReactiveSystem = source ~> route ~> sink
```

Or alternatively the DSL exposes also:

```scala

val reactiveSys: ReactiveSystem = source via route to sink
```

Or more basic:

```scala

val reactiveSystem: ReactiveSystem = ReactiveSystem(source, route, sink)
```


Reactive Source
---------------

A reactive source is the inbound gateway from which request messages are processed by the reactive system.
As part of the Kafka-Reactive-System there is a KafkaReactiveSource object which provides a method create to build
a reactive source able to consume from a Kafka topic.

```
   _________________       ______________________________________ . _ . _ _ 
  |      KAFKA      |     |                                 REACTIVE SYSTEM 
  |     _______     |     |     ________________________________     
  |    | topic |~~~~~~~~~~~~~~>|  Source[KafkaRequestEnvelope]  |
  |    '-------'    |     |    |________________________________|
  |_________________|     |_______________________________________ . _ . _ .
  

```

*Scala API:*

```scala

val source: Source[KafkaRequestEnvelope, _] = ReactiveKafkaSource.create(
  requestTopic = "simpleService", 
  bootstrapServers = Set("localhost:9092"), 
  clientId: "simpleServiceClient1"
)

```

Reactive Route
--------------

The reactive route is the component in charge to analise the KafkaRequestEnvelope message and dispatch the request to the relevant service.
In order to accomplish to its task the reactive route must know all the reactive services we want to expose.

A reactive route can be though as a mapping between the Reactive Service URI and its current implementation:

``` 
                                    
                                        /---->{Service: echo}
                                        |                    
             __________                 |                    
            |          |----------------'                    
            | Reactive |                                     
            |  Route   |------------------------>{Service: size}
            |          |                                        
            '----------'                                        
                    |                                           
                    \--------------->{Service: reverse}         
                                   
                                      
```

### Reactive Service

The reactive service represents the entry point of your business logic. A reactive service is nothing more than a function which has an input 
and can produce a result, because the input and output are delivered through messages the input type must be deserializable and the output serializable.

*Scala API:*

```scala

case class ReactiveService[-In: ReactiveDeserializer, +Out: ReactiveSerializer](id: String)(f: In => Future[Error Xor Out])

```

From the Scala API you can notice that the service has a unique DI, the execution of the ReactiveService is "intrinsicly" asynchronous and might return a business error.  

### Reactive Route DSL

In order to make it easy to create a ReactiveRoute with all its services associated a specific dsl has been developed.

*Scala API:*

```scala

val route: ReactiveRoute = request.aSync[String, String]("echo") { in => 
    s"echoing: $in"
  } ~ request.aSync("size") { (in: String) =>
    in.length
  } ~ request.sync("reverse") { (in: String) =>
    in.reverse
  }

```

#### Route DSL: Future Flatten

...

#### Route DSL: Handle Xor

...

Reactive Sink
-------------

...

Reactive Client
---------------

...

### Reactive Serializer/Deserializer

...

### Actor Per Request - Correlation Header

...