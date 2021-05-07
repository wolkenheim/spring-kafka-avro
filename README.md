# spring-kafka-avro
Sample Setup for Spring boot with Apache Kafka and Avro

Disclaimer: this is not a tutorial for beginners. The Apache Kafka ecosystem is vast and not easy to master. It helped me a lot to take the courses of Stephane Maarek on Udemy
to understand how it worked. Sorry to bear the news: free tutorials are not enough.

Start with a simple Spring Boot Maven application from https://start.spring.io/ 

## 1. Apache Avro
Now we are going to add some dependencies: Apache Avro. This is a JSON-like schema syntax to serialize and deserialize data. When we send our products
via Kafka topics they will be encoded with Avro schemas.

How does it work? We compile POJOs out of avro files. Let´s get started writing a domain object as a Avro file. This follows basically
the idea you find at the official docs: https://avro.apache.org/docs/current/gettingstartedjava.html
Let´s add some fields to the file resources/avro/product.avsc: string, integer, nullable union types, enum, lists.

`mvn clean compile` will generate classes under target/generated-sources/avro...
Let´s write a quick test to make sure the classes have been generated: test/java/cloud.wolkenheim.springbootkafkaavro.avro/ProductTest

## 2. Add a Kafka cluster for local development
A cluster consists of a zookeeper instance for the config files plus one to many brokers. As we just need a simple setup, one broker is sufficient.
To work with Avro and Kafka the schema registry is used. This is an expansion by Confluent. You will find the basic workflow here:
https://docs.confluent.io/platform/current/schema-registry/index.html Also a quick note here that Confluent developed quite a few docker containers
for Kafka that you could use here. Like the Schema Registry UI which I added and can be accessed at http://localhost:8000/#/

Start the local cluster with
```
cd _INFRA/dev/kafka && docker-compose up
```

## 3. Write a avro producer
We use autoconfigure magic to create a producer bean from application.yaml. You will need the maven package io.confluent.kafka-avro-serializer
here as well. Start with the most basic avro setup. A simple REST controller endpoint that produces a test product every time GET http://localhost:8080/product
is called.

Inside the source code there is ridiculously little to do. The producer is just `KafkaTemplate<String, Product> kafkaTemplate;` with a send message. 

Now for the debugging. The schema should be registered automatically once product is produced. Either use the GUI tool http://localhost:8000/#/cluster/default/schema/product-topic-value/version/1
Or just straight in the schema registry with `curl http://localhost:8081/subjects` to list all topics, get the versions of the schema `curl http://localhost:8081/subjects/product-topic-value/versions`
and finally retrieve the schema with `curl http://localhost:8081/subjects/product-topic-value/versions/1`

How to check if the message has been produced? Avro is binary data so the regular kafka-console-consumer will not be very useful here. Luckily
Confluent shipped a kafka-avro-console-consumer tool with the schema-registry Docker image which I found extremely helpful.
```
docker exec schema-registry kafka-avro-console-consumer --bootstrap-server broker:9092 --topic product-topic --from-beginning
```
Success! You will find the test product here and see new ones arriving whenever you call the controller endpoint.

Now that we have a proof of concept it would be good to clean up a bit. The producer should be moved to its own class. And the Controller endpoint
should be a POST method and receive product data. This should be easily done be just changing the method signature to
`void publishProduct(@RequestBody Product product)`. In a real world scenario you would use an DTO object here, validate it and map it 
to the specific Product object. For now this is sufficient, and we can use Postman to call the endpoint:
```
curl --location --request POST 'localhost:8080/product' \
--header 'Content-Type: application/json' \
--data-raw '{"id" : "12345", "name": "my shoe", "description": "goes here", "state": "ACTIVE", "crossSellingIds" : ["42332"]}'
```
Now all the producer logic abides in its own @Service. Freshly produced products via Postman should be visible in the kafka-avro-console-consumer.

## 4. Add an avro consumer
It is basically one annotation 
```
@KafkaListener(topics = "${topic.product}")
public void consume(ConsumerRecord<String, Product> record) {
Product product = record.value();
}
```
There need to be autoconfiguration values added in kafka.yaml as well.
When starting up the application all records should be consumed automatically. The acknowledgement aka ack is sent so when starting up
next time no records will be consumed, unless there are more products produced.
If you run into the `nested exception is java.lang.ClassCastException: class org.apache.avro.generic.GenericData$Record cannot be cast to class`
you forgot to set the property `spring.kafka.consumer.properties.specific.avro.reader=true`.
If you are only looking for the body aka value of the message but don´t mind metadata and key, your listener can be reduced to 
```
@KafkaListener(topics = "${topic.product}")
public void consume(Product product) {}
```

---

https://www.confluent.io/blog/schema-registry-avro-in-spring-boot-application-tutorial/