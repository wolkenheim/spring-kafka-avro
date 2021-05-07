package cloud.wolkenheim.springbootkafkaavro.advice;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorMessage {
    private String type;
    private String message;
}
