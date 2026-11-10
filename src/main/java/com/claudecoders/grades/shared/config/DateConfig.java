package com.claudecoders.grades.shared.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration(proxyBeanMethods = false)
public class DateConfig {
    @Bean
    Clock businessClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }

    @Bean
    DateTimeProvider businessDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
