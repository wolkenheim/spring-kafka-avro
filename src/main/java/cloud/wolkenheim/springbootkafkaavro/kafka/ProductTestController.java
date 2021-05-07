package cloud.wolkenheim.springbootkafkaavro.kafka;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.ProductState;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("product")
public class ProductTestController {

    protected final ProductProducer productProducer;

    public ProductTestController(ProductProducer productProducer) {
        this.productProducer = productProducer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    void publishProduct(@RequestBody Product product){
        productProducer.send(product);
    }
}
