package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;
import static org.mockito.Mockito.mock;
public class ProductConsumerTest {

    ProductConsumer productConsumer;

    Acknowledgment ack = mock(Acknowledgment.class);

    @BeforeEach
    void setUp(){
        productConsumer = new ProductConsumer();
    }

    @Test
    void shouldConsume(){
        Product product = ProductFactory.getTestProduct();
        productConsumer.consume(product, ack);
    }
}
