package com.claudecoders.grades.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(
                    authorize ->
                            authorize
                                    .requestMatchers(
                                            "/health",
                                            "/actuator/health",
                                            "/actuator/health/**",
                                            "/api/docs",
                                            "/api/docs/**",
                                            "/api/swagger-ui/**",
                                            "/swagger-ui/**",
                                            "/v3/api-docs/**")
                                    .permitAll()
                                    .anyRequest()
                                    .authenticated());
        }

        return http.build();
    }
}
