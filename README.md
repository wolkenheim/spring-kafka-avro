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

---
