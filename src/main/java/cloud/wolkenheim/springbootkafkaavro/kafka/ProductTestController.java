package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.ProductState;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("product")
public class ProductTestController {

    private static final Logger log = LoggerFactory.getLogger(ProductTestController.class);

    @Value("${topic.product}")
    private String topic;

    public ProductTestController(KafkaTemplate<String, Product> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final KafkaTemplate<String, Product> kafkaTemplate;

    @GetMapping
    void publishProduct(){

        Product product = getTestProduct();

        try {
            kafkaTemplate.send(
                    topic,
                    product.getId().toString(),
                    product
            );
        } catch (SerializationException e){
            log.error("KAFKA Serialization ERROR:" + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            log.error(sStackTrace);
        } catch (Exception e){
            log.error("KAFKA ERROR:" + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            log.error(sStackTrace);
        }
    }

    protected Product getTestProduct(){
        return Product.newBuilder()
                .setId("12345")
                .setName("shoe 355")
                .setDescription("Brand new Shoe")
                .setState(ProductState.ACTIVE)
                .setQty(9)
                .setCrossSellingIds(new ArrayList<>(Arrays.asList("23456", "34566")))
                .build();
    }
}
