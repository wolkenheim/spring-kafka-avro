package cloud.wolkenheim.springbootkafkaavro.helper;

import cloud.wolkenheim.springbootkafkaavro.Product;
import cloud.wolkenheim.springbootkafkaavro.ProductState;

import java.util.ArrayList;
import java.util.Arrays;

public class ProductFactory {

    public static Product getTestProduct(){
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
