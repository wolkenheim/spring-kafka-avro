package cloud.wolkenheim.springbootkafkaavro.service;

import cloud.wolkenheim.springbootkafkaavro.Product;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceMockImpl implements ProductService {

    @Override
    public void save(Product product){
        // this is a mock implementation as this demo project does not have a database running
        // the method could call ProductRepository.save()
        // do nothing
    }
}
