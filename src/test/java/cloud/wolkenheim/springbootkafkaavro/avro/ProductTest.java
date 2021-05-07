package cloud.wolkenheim.springbootkafkaavro.avro;

import cloud.wolkenheim.springbootkafkaavro.ProductState;
import org.junit.jupiter.api.Test;
import cloud.wolkenheim.springbootkafkaavro.Product;

import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ProductTest {

    @Test
    void generatedClassExists(){
        Product product = Product.newBuilder()
                .setId("12345")
                .setName("shoe 355")
                .setDescription("Brand new Shoe")
                .setState(ProductState.ACTIVE)
                .setQty(9)
                .setCrossSellingIds(new ArrayList<>(Arrays.asList("23456", "34566")))
        .build();

        assertEquals(product.getId(), "12345");
    }
}
