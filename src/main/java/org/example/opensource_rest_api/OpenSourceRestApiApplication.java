package org.example.opensource_rest_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class OpenSourceRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenSourceRestApiApplication.class, args);
    }

}
