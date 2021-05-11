package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import cloud.wolkenheim.springbootkafkaavro.service.ProductService;
import cloud.wolkenheim.springbootkafkaavro.service.ProductServiceMockImpl;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = {ProductConsumer.class, KafkaAutoConfiguration.class })
@EmbeddedKafka(partitions = 1, topics = {"test-product-topic"}, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@ActiveProfiles("kafka-consumer-test")
@DirtiesContext
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
public class ProductConsumerIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker kafkaEmbedded;

    KafkaTemplate<String, Product> kafkaTemplate;

    @MockBean
    ProductService productService;


    @BeforeEach
    void setUp(){
        buildProducer();
    }

    @Test
    void shouldConsume() throws Exception {
        kafkaTemplate.send("test-product-topic", "id_1", ProductFactory.getTestProduct());

        TimeUnit.SECONDS.sleep(1);

        ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);
        verify(productService, times(1)).save(argument.capture());
    }

    protected void buildProducer(){
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(kafkaEmbedded));

        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        configs.put("schema.registry.url", "mock://localhost");

        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, Product>(configs));
        kafkaTemplate.setDefaultTopic("test-product-topic");
    }

}
