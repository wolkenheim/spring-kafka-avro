package cloud.wolkenheim.springbootkafkaavro.kafka;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("schema-registry")
public class SchemaRegistryController {

    protected final RegisterAvroClient registerAvroClient;

    public SchemaRegistryController(RegisterAvroClient registerAvroClient) {
        this.registerAvroClient = registerAvroClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    void register(){
        registerAvroClient.send();
    }
}
