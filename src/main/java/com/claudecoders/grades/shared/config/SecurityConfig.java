package com.claudecoders.grades.shared.config;

import com.claudecoders.grades.auth.RegisteredUserFilter;
import com.claudecoders.grades.shared.exception.ApiError;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    FilterRegistrationBean<RegisteredUserFilter> registeredUserFilterRegistration(
            RegisteredUserFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment environment,
            RegisteredUserFilter registeredUserFilter,
            JsonMapper mapper)
            throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, exception) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(
                            response.getWriter(),
                            new ApiError(
                                    Instant.now(),
                                    401,
                                    "Unauthorized",
                                    "Se requiere un JWT válido",
                                    Map.of()));
                };
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.authenticationEntryPoint(authenticationEntryPoint)
                                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(
                                                (request, response, exception) -> {
                                                    response.setStatus(
                                                            HttpStatus.FORBIDDEN.value());
                                                    response.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    mapper.writeValue(
                                                            response.getWriter(),
                                                            new ApiError(
                                                                    Instant.now(),
                                                                    403,
                                                                    "Forbidden",
                                                                    "Acceso denegado",
                                                                    Map.of()));
                                                }))
                .addFilterAfter(registeredUserFilter, BearerTokenAuthenticationFilter.class);

        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            http.authorizeHttpRequests(
                    authorize ->
                            authorize
                                    .requestMatchers(
                                            HttpMethod.POST,
                                            "/api/v1/expedients",
                                            "/api/v1/expedients/**")
                                    .authenticated()
                                    .requestMatchers(HttpMethod.PUT, "/api/v1/expedients/**")
                                    .authenticated()
                                    .requestMatchers(HttpMethod.PATCH, "/api/v1/expedients/**")
                                    .authenticated()
                                    .requestMatchers(HttpMethod.DELETE, "/api/v1/expedients/**")
                                    .authenticated()
                                    .anyRequest()
                                    .permitAll());
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
