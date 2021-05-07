package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class ProductConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @KafkaListener(topics = "${topic.product}")
    public void consume(@Payload Product product, Acknowledgment ack) {
        log.info("CONSUMED: " + product.getName());
        ack.acknowledge();
    }
}
