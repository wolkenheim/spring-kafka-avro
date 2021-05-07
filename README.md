# spring-kafka-avro
Sample Setup for Spring boot with Apache Kafka and Avro


Start with a simple Spring Boot Maven application from https://start.spring.io/ 

## 1. Apache Avro
Now we are going to add some dependencies: Apache Avro.
This is used to compile POJOs out of avro files. Let´s get started writing a domain object as a Avro file. This follows basically
the idea you find at the official docs: https://avro.apache.org/docs/current/gettingstartedjava.html
Let´s add some fields to the file resources/avro/product.avsc: string, integer, nullable union types, enum, lists.

`mvn clean compile` will generate classes under target/generated-sources/avro...
Let´s write a quick test to make sure the classes have been generated: test/java/cloud.wolkenheim.springbootkafkaavro.avro/ProductTest
