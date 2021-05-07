package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.springframework.kafka.core.KafkaTemplate;
import static org.mockito.Mockito.mock;

public class ProductProducerTest {

    ProductProducer productProducer;

    KafkaTemplate<String, Product> kafkaTemplate = mock(KafkaTemplate.class);

    @BeforeEach
    void setUp(){
        productProducer = new ProductProducer(kafkaTemplate);
    }

    @Test
    void shouldProduce(){
        ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);

        Product product = ProductFactory.getTestProduct();
        productProducer.send(product);

        verify(kafkaTemplate, times(1)).send(any(), any(), argument.capture());
    }
}
