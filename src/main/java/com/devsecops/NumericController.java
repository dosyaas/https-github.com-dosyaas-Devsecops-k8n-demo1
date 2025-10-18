package com.devsecops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Простейший REST-контроллер:
 *  GET /                      -> "Kubernetes DevSecOps"
 *  GET /compare/{value}       -> текст сравнения с 50
 *  GET /increment/{value}     -> проксирует в NodeJS-сервис /plusone и возвращает число
 */
@RestController
public class NumericController {

    private static final Logger logger = LoggerFactory.getLogger(NumericController.class);

    // Можно задавать через переменную окружения NODE_SERVICE_URL
    // или application.properties: node.service.url=http://node-service:5000/plusone
    @Value("${node.service.url:http://node-service:5000/plusone}")
    private String baseURL;

    private final RestTemplate restTemplate;

    public NumericController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/")
    public String welcome() {
        return "Kubernetes DevSecOps";
    }

    @GetMapping("/compare/{value}")
    public String compareToFifty(@PathVariable int value) {
        if (value > 50) {
            return "Greater than 50";
        } else {
            return "Smaller than or equal to 50";
        }
    }

    @GetMapping("/increment/{value}")
    public int increment(@PathVariable int value) {
        String url = baseURL + "/" + value;
        logger.info("Calling Node service: {}", url);

        ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(url, String.class);

        String body = responseEntity.getBody();
        logger.info("Value Received in Request - {}", value);
        logger.info("Node Service Response - {}", body);

        return Integer.parseInt(body);
    }
}
