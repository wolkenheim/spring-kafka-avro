package cloud.wolkenheim.springbootkafkaavro.avro;

import cloud.wolkenheim.springbootkafkaavro.helper.ProductFactory;
import cloud.wolkenheim.springbootkafkaavro.kafka.RegisterAvroClient;
import org.junit.jupiter.api.Test;
import cloud.wolkenheim.springbootkafkaavro.Product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.boot.web.client.RestTemplateBuilder;


public class ProductTest {

    @Test
    void generatedClassExists(){
        Product product = ProductFactory.getTestProduct();
        assertEquals(product.getId(), "12345");

        RegisterAvroClient registerAvroClient = new RegisterAvroClient(new RestTemplateBuilder());
        String result = registerAvroClient.readAndSerializeAvroSchema();

        result.replaceAll("\"","\\\"");
        String payload = String.format("{ \"schema\" : \"%s\"}", result);

        System.out.print(payload);
    }
}
