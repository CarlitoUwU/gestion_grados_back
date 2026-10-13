package com.claudecoders.grades.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DateConfig {
    @Bean
    Clock businessClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
