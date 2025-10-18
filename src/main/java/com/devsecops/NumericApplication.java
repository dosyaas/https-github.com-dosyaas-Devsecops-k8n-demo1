package com.devsecops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class NumericApplication {

    public static void main(String[] args) {
        SpringApplication.run(NumericApplication.class, args);
    }

    // ✅ добавляем бин, чтобы Spring мог инжектить RestTemplate в контроллер
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
