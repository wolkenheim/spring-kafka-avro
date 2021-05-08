package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = {ProductController.class, ProductProducer.class, KafkaAutoConfiguration.class })
@EmbeddedKafka(partitions = 1, topics = {"test-product-topic"}, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@AutoConfigureMockMvc
@EnableWebMvc
@ActiveProfiles("test")
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ProductConsumerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EmbeddedKafkaBroker kafkaEmbedded;

    Consumer<String, Product> productConsumer;

    @BeforeEach
    void setUp(){
        buildConsumer();
    }

    @AfterEach
    void tearDown(){
        productConsumer.close();
    }

    @Test
    void shouldConsume() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"id\" : \"12345\", \"name\": \"my shoe\", \"description\": \"goes here\", \"state\": \"ACTIVE\", \"crossSellingIds\" : [\"42332\"]}")

        )
                // assert
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        ConsumerRecord<String, Product> record = KafkaTestUtils.getSingleRecord(productConsumer, "test-product-topic", 500);

        assertEquals(record.value().getId().toString(), "12345");
    }

    protected void buildConsumer(){
        //consumers used in test code needs to be created like this in code because otherwise it won't work
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("in-test-consumer", "false", kafkaEmbedded));
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configs.put("schema.registry.url", "mock://localhost:9081");
        configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        productConsumer = new DefaultKafkaConsumerFactory<String, Product>(configs).createConsumer("my-group-1", "10");
        productConsumer.subscribe(Lists.newArrayList("test-product-topic"));
    }
}
