package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class ProductProducer {

    @Value("${topic.product}")
    private String topic;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final KafkaTemplate<String, Product> kafkaTemplate;

    public ProductProducer(KafkaTemplate<String, Product> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(Product product){
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
}
