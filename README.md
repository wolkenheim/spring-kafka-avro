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

One important note: The default value of the Maven plugin for String is not String but CharSequence, the underlying 
interface of String in Java. In case you would like to use String instead you can set the configuration stringType to String.
As usual, there is a catch. During Serialization/ Deserialization of records Avro schemas are exchanged with the Schema Registry. 
The plugin will add Java-specific annotations to the avro schema: `"avro.java.string": "String"`. In case you have also non-java
consumers / producers in your network be careful with this option. 

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

Now for the debugging. The schema should be registered automatically once product is produced. Either use the GUI tool 
http://localhost:8000/#/cluster/default/schema/product-topic-value/version/1
Or just straight in the schema registry with `curl http://localhost:8081/subjects` to list all topics, get the versions 
of the schema `curl http://localhost:8081/subjects/product-topic-value/versions`
and finally retrieve the schema with `curl http://localhost:8081/subjects/product-topic-value/versions/1`
The full list of all available REST endpoints can be found at: https://docs.confluent.io/platform/current/schema-registry/develop/api.html

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
Writing a test is simple. The consumer is just a method expecting a product object and an acknowledgement. The second one can be
mocked via `Acknowledgment ack = mock(Acknowledgment.class);`. For the test product I introduced a factory helper.
The test is pretty straight forward. You get the logging message. Now in a real application something is done with the consumed
message. E.g. saved to a database via a service and repository. So the idea here would be spying on the arguments of the
service and asserting that they are what we expect them to be. I will be honest here - this is quite redundant. Passing in object
in and expecting the same object to be passed on. However, it is a test at least.

## 6. Unit test for the producer
A simple test mocks the KafkaTemplate, captures its argument and asserts that the producer has passed the Product to the KafkaTemplate.
Nothing fancy is happening here.

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
```java
        mockMvc.perform(
        MockMvcRequestBuilders.post("/product")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content("{\"id\" : \"12345\", \"name\": \"my shoe\", \"description\": \"goes here\", \"state\": \"ACTIVE\", \"crossSellingIds\" : [\"42332\"]}")

        )
        .andExpect(status().isOk());
```
If you ever run into a 415 Status Code response, it can be fixed using the 
`@EnableWebMvc` as suggest here: https://stackoverflow.com/questions/32838699/spring-mvc-testing-results-in-415-error

### 7.2 Making the Schema Registry work
With the @EmbeddedKafka annotation an in-memory kafka cluster can be instantiated for tests. There is a
catch here: the schema registry is missing. Behind the scene io.confluent.kafka.serializers.KafkaAvroSerializer and 
KafkaAvroDeSerializer
are trying to make http calls to it. There is none, therefore the test will result in 
`Caused by: org.apache.kafka.common.errors.SerializationException: Error deserializing Avro message for id 1`
The obvious idea here is to write an own mock implementation of the Serializer/Deserializer. Someone did that already 
and that solution works: https://medium.com/@igorvlahek1/no-need-for-schema-registry-in-your-spring-kafka-tests-a5b81468a0e1

There is a much quicker way though as someone pointed out in the comments of said article. Just use "mock" instead of 
"http" in your configs for the producer / consumer: `schema.registry.url: mock://localhost`
No hassle with writing and maintaining mock implementations.

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

Now retrieving the record is just a one-liner:
```java
ConsumerRecord<String, Product> record = KafkaTestUtils.getSingleRecord(productConsumer, "test-product-topic", 500);
```
Assertions can be written easily here simple as `assertEquals(record.value().getId().toString(), "12345");`

There is an alternative approach though. Not wiring in your own consumer but working directly with a listener on the
Kafka container. It has been described here: https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/



---

## Helpful resources:

https://docs.spring.io/spring-kafka/reference/html/#reference

https://www.confluent.io/blog/schema-registry-avro-in-spring-boot-application-tutorial/
