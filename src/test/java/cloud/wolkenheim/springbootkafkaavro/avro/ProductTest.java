package cloud.wolkenheim.springbootkafkaavro.avro;

import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import org.junit.jupiter.api.Test;
import cloud.wolkenheim.springbootkafkaavro.Product;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ProductTest {

    @Test
    void generatedClassExists(){
        Product product = ProductFactory.getTestProduct();
        assertEquals(product.getId(), "12345");
    }
}
