package cloud.wolkenheim.springbootkafkaavro.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class RegisterAvroClient {

    protected String apiEndpoint = "http://localhost:8081/subjects/test3/versions";

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    protected final RestTemplate restTemplate;

    public RegisterAvroClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public void send() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String schemaString = readAndSerializeAvroSchema();
        schemaString = schemaString
                .replace("\t", "")
                .replace("\n", "")
                .replace("\"", "\\\"");

        String payload = String.format("{ \"schema\" : \"%s\"}", schemaString);

        log.info("PAYLOAD:" + payload);

        HttpEntity<String> request = new HttpEntity<String>(payload, headers);

        try {
            restTemplate.postForObject(apiEndpoint, request, String.class);
            log.info("Published product schema in registry");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public String readAndSerializeAvroSchema() {
        String data = "";
        try {
            byte[] dataArr = FileCopyUtils.copyToByteArray(new ClassPathResource("avro/product.avsc").getInputStream());
            data = new String(dataArr, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not read testdata/sdb_product_v3.json");
        }
        return data;
    }
}
