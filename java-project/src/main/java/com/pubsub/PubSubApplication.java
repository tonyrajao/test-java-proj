package com.pubsub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.pubsub")
@EnableJpaRepositories(basePackages = "com.pubsub.repository")
@EntityScan(basePackages = "com.pubsub.model")
public class PubSubApplication {
    public static void main(String[] args) {
        SpringApplication.run(PubSubApplication.class, args);
    }
}
