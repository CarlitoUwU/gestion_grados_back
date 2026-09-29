package com.claudecoders.grades;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GradesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradesApplication.class, args);
    }
}
