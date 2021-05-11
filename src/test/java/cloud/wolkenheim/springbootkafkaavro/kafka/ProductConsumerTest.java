package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import cloud.wolkenheim.springbootkafkaavro.service.ProductService;
import cloud.wolkenheim.springbootkafkaavro.service.ProductServiceMockImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
public class ProductConsumerTest {

    ProductConsumer productConsumer;

    Acknowledgment ack = mock(Acknowledgment.class);

    ProductService productService = mock(ProductServiceMockImpl.class);

    @BeforeEach
    void setUp(){
        productConsumer = new ProductConsumer(productService);
    }

    @Test
    void shouldConsume(){
        Product product = ProductFactory.getTestProduct();
        productConsumer.consume(product, ack);

        ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);
        verify(productService, times(1)).save(argument.capture());
    }
}
