# Spring Boot with Apache Kafka, Apache Avro and the Confluent Schema Registry
Small sample project with a sample setup to make these components work together.

Disclaimer: this is not a tutorial aimed at beginners. The Apache Kafka ecosystem is vast and not easy to master. It helped me a lot to take the courses of Stephane Maarek on Udemy
to gain a good understanding.

Start with a simple Spring Boot Maven application from https://start.spring.io/ 

## 1. Apache Avro
Now we are going to add some dependencies: Apache Avro. This is a JSON-like schema syntax to serialize and deserialize data. When we send our products
via Kafka topics they will be encoded with Avro schemas.

How does it work? Similar to Protocol Buffers. There will be POJOs compiled from the avro files. Now let´s started writing a domain object as a Avro file. This follows basically
the idea you find at the official docs: https://avro.apache.org/docs/current/gettingstartedjava.html
We can add some fields to the file resources/avro/product.avsc: string, integer, nullable union types, enum, lists.

`mvn clean compile` will generate classes at target/generated-sources/avro...
Let´s write a quick test to make sure the classes have been generated: test/java/cloud.wolkenheim.springbootkafkaavro.avro/ProductTest

One important note: The default value of the avro-maven-plugin for String is not String but CharSequence, the underlying 
interface of class String in Java. In case you would like to use String instead you can set the configuration stringType to String.
As you might have guessed, there is a catch. During Serialization/ Deserialization of records Avro schemas are exchanged with the Schema Registry. 
The plugin option will add Java-specific annotations to the avro schema: `"avro.java.string": "String"`. In case you have also non-java
consumers / producers in your network be careful with this option. This is especially the case if your organization 
publishes its schemas via a Git repository and a CI pipeline to the Schema Registry.
Read the discussion about this here: https://github.com/confluentinc/schema-registry/issues/868

## 2. Add a Kafka cluster for local development
A cluster consists of a Zookeeper instance plus one to many brokers. As we just need a simple setup, one broker is sufficient.
To work with Avro and Kafka the schema registry is needed. This is an expansion of Kafka by Confluent. You will find the basic workflow here:
https://docs.confluent.io/platform/current/schema-registry/index.html Also a quick note here that Confluent developed quite a few docker containers
for Kafka that you could use here. Like the Schema Registry UI which I added and can be accessed at http://localhost:8000/#/

Start the local cluster with
```
cd _INFRA/dev/kafka && docker-compose up
```

## 3. Write an Avro producer
We use autoconfigure magic to create a producer bean from application.yaml. You will need the maven package io.confluent.kafka-avro-serializer
here as well. Start with the most basic avro setup. A simple REST controller endpoint that produces a test product every time GET http://localhost:8080/product
is called.

Inside the source code there is ridiculously little to do. The producer is just `KafkaTemplate<String, Product> kafkaTemplate;` with a send message. 

Now for the debugging. The schema should be registered automatically once product is produced. Either use the GUI tool 
http://localhost:8000/#/cluster/default/schema/product-topic-value/version/1
Or just straight in the schema registry with `curl http://localhost:8081/subjects` to list all topics, get the versions 
of the schema `curl http://localhost:8081/subjects/product-topic-value/versions`
and finally retrieve the schema with `curl http://localhost:8081/subjects/product-topic-value/versions/1`
The full list of all available REST endpoints can be found at: https://docs.confluent.io/platform/current/schema-registry/develop/api.html

How to check if the message has been produced? Avro is binary data and therefore the regular kafka-console-consumer will not be very useful. 
Luckily Confluent ships a kafka-avro-console-consumer cli tool with the schema-registry Docker image. It can be accessed
like this
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
Now all the producer logic abides in its own Service. Freshly produced products via Postman should be visible in the kafka-avro-console-consumer.

## 4. Add an Avro consumer
Now we can add a consumer inside the Spring Boot application. This is basically one annotation:
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
you forgot to set the property `spring.kafka.consumer.properties.specific.avro.reader=true`. This is hardly ever mentioned and can result in 
hours of debugging your configuration.
If you are only looking for the body aka value of the message but don´t mind metadata and key, your listener can be reduced to 
```
@KafkaListener(topics = "${topic.product}")
public void consume(Product product) {}
```

In this setup the acknowledgements are sent automatically. Now in case you want to set those manually you need to set
spring.kafka.listener.ack-mode=MANUAL_IMMEDIATE
To check if it worked just produce a few messages and restart the application. The messages should be consumed every time
the application restarts, hence the ack is now supposed to be set manually which is not implemented yet.

Change the Listener to 
```
public void consume(Product product, Acknowledgment ack){
        log.info("CONSUMED: " + product.getName());
        ack.acknowledge();
```
Now on the first restart all messages are getting consumed and acknowledged, after the second restart no more messages
are consumed.

## 5. Unit Test for the consumer
Writing a unit test is easy. The consumer is just a method expecting a payload (and an acknowledgement). The second one can be
mocked via `Acknowledgment ack = mock(Acknowledgment.class);`. For the test product I introduced a factory helper.
The test is pretty straight forward. You get the logging message. Now in a real application something is done with the consumed
message. E.g. saved to a database via a service and repository. So the idea here would be spying on the arguments of the
service and asserting that they are what we expect them to be.

## 6. Unit test for the producer
Again: a simple unit test without any Spring context. The KafkaTemplate instance is mocked, captures its argument and 
asserts that the producer has passed the Product to the KafkaTemplate.

## 7. Integration Test of the Kafka Producer with Embedded Kafka
Now for the integration test with junit5. We would like to test posting a product payload to the controller. The payload 
should get send to kafka. We will verify the result by consuming the topic and retrieving the Avro encoded message.

The source code of the test can be found at src/test/java/cloud/wolkenheim/springbootkafkaavro/kafka/ProductControllerIntegrationTest

### 7.1 Setting up the application context
This is an integration test. We need to use the `@SpringBootTest` annotation There might be other dependencies (e.g. databases) 
that are not present. It is sufficient to boot up only the classes we need here. That is the controller and producer in 
the application and the `@EmbeddedKafka` as an in-memory mock of an external dependency. However, running the test with
`@SpringBootTest(classes = {ProductController.class, ProductProducer.class })` will result in a NoSuchBeanDefinitionException. The
test context cannot build an instance of KafkaTemplate. To solve this we need to add KafkaAutoConfiguration to the list
of classes. There is no configuration file though. We should add one at resource/application-kafka-producer-test.yaml and load it with
`@ActiveProfiles("kafka-producer-test")`. It has to include the configuration matching the embedded kafka.#

### 7.1 Setting up the testing tools
We can use MockMvc to test our RestController. The endpoint is returning status code 200 hence there is not much to 
asserted or verified here. All we do is send our payload to the endpoint:
```
        mockMvc.perform(
        MockMvcRequestBuilders.post("/product")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content("{\"id\" : \"12345\", \"name\": \"my shoe\", \"description\": \"goes here\", \"state\": \"ACTIVE\", \"crossSellingIds\" : [\"42332\"]}")

        )
        .andExpect(status().isOk());
```
If you ever run into a 415 Status Code response, it can be fixed using the 
`@EnableWebMvc` annotation as suggest here: https://stackoverflow.com/questions/32838699/spring-mvc-testing-results-in-415-error

### 7.2 Making the Schema Registry work
With the @EmbeddedKafka annotation an in-memory kafka cluster can be instantiated for tests. There is a
catch here: the schema registry is missing. Behind the scene io.confluent.kafka.serializers.KafkaAvroSerializer and 
KafkaAvroDeSerializer are trying to make http calls to it. There is none during testing, therefore the test will result in 
`[...] Caused by: org.apache.kafka.common.errors.SerializationException: Error deserializing Avro message for id 1`
The obvious idea here is to write an own mock implementation of the Serializer/ Deserializer. Someone did that already 
and that solution works: https://medium.com/@igorvlahek1/no-need-for-schema-registry-in-your-spring-kafka-tests-a5b81468a0e1

There is a much quicker way though as another person pointed out in the comments of that article. Just use "mock" instead of 
"http" in your configs for the producer / consumer: `schema.registry.url: mock://localhost`. This is a testing option
integrated by Confluent. No hassle with writing and maintaining mock implementations.

### 7.3 Build a Kafka Consumer for Testing
What we got so far: a JSON payload is sent to the controller endpoint which calls the Kafka producer and sends the message. 
Let´s assume for a minute our application does not run a consumer. In a real-world scenario this is quite likely as the
service itself knows its own data already. We should set up a consumer inside the test to mock it. Time to use the 
`@EmbeddedKafka` annotation and autowire `EmbeddedKafkaBroker kafkaEmbedded;` inside the test class.

```java
    protected void buildConsumer(){
        // see: https://docs.spring.io/spring-kafka/reference/html/#junit
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("in-test-consumer", "true", kafkaEmbedded));

        // apache avro is not part of the KafkaTestUtils so the attributes need to be set manually
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        configs.put("schema.registry.url", "mock://localhost");

        productConsumer = new DefaultKafkaConsumerFactory<String, Product>(configs).createConsumer("my-group-1", "10");
        productConsumer.subscribe(Lists.newArrayList("test-product-topic"));
    }
```

There is an alternative approach though. Not wiring in your own consumer but working directly with a listener on the
Kafka container. It is described here: https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/

Now retrieving the record is just a one-liner:
```java
ConsumerRecord<String, Product> record = KafkaTestUtils.getSingleRecord(productConsumer, "test-product-topic", 500);
```
Assertions can be written easily here simple as `assertEquals(record.value().getId().toString(), "12345");` Are there
any more assertions to be made here? I would argue: not really. All I´m interested in as the fact that I can proof
my record is sent and received. If I want to test correct mappings of attributes, I would do that inside the producer. 
No Kafka needed for that. I expect the Serialization / Deserialization process to work correctly.

## 8. Integration Test of the Kafka Consumer with Embedded Kafka
Build a test producer, publish a test product. Then start the Spring Application Context with the consumer and get the 
message. There is another catch here: we run into race conditions. Spring Context has no way to know that it should run
until a message is produced in the test context, can be consumed, and has been consumed. An easy but brute way to fix 
this is adding a timeout to the test. 

## 8. Evaluation of the testing strategy
We could spin this further and take it to the next step from here: how about Testcontainers instead of the embedded Kafka 
cluster. However, it begs the question: does this whole testing concept make sense? I would argue: up to Unit tests yes
it does. Further on: no, it does not. To make it crystal clear: when it comes to producing everything inside 
KafkaTemplate.send() should be none of our business. This is the realm of Kafka and its java libraries.
We´re not testing our own source code, but the source code of someone else and most of all configuration. The presented 
configuration with its implicit assumptions in the integration test scenario is both oversimplifying and too complex. 
It is too complex as we simply need to know a payload is passed to a method. Or in the case of a consumer: a 
certain payload is received. It is oversimplifying as this is not a real Kafka setup. There are more configuration 
options involved. 

To give you an idea what kind of bugs I have encountered in a real-world deployments: wrong named or missing variables 
in CI pipeline. Secrets not set correctly in Kubernetes. Basic Auth Credentials for Schema Registry not working. Wrong 
Keystore for SSL Certificates. The list goes on. The integration tests presented here would not have prevented those 
bugs. Again: all knowledge that is gained by those tests here is that Object goes in, Object comes out. This is
different to testing a database integration, no matter if relational or non-relational. Here I would like to know if
queries are delivering correct results. When testing JPA repositories pure unit tests without an embedded database 
are not very useful.

However, I would argue that those tests are very useful base to build up for more complex setups: when using
Kafka Streams and Ksql. Here you have definitely the use case for mocked test data in kafka.

To sum this up: for my current use case of Kafka unit test are sufficient. If 

---

## Other helpful resources:

https://docs.spring.io/spring-kafka/reference/html/#reference

https://www.confluent.io/blog/schema-registry-avro-in-spring-boot-application-tutorial/
