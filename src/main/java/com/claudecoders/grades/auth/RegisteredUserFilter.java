package com.claudecoders.grades.auth;

import com.claudecoders.grades.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RegisteredUserFilter extends OncePerRequestFilter {
    private final CurrentUserService currentUser;
    private final Environment environment;
    private final JsonMapper mapper;

    public RegisteredUserFilter(
            CurrentUserService currentUser, Environment environment, JsonMapper mapper) {
        this.currentUser = currentUser;
        this.environment = environment;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            var user = currentUser.find(authentication);
            if (user.isEmpty() || !user.get().isActive()) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(
                        response.getWriter(),
                        new ApiError(
                                Instant.now(),
                                403,
                                "Forbidden",
                                "El usuario autenticado no está registrado o activo",
                                Map.of()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))
                && !path.startsWith("/api/v1/expedients")) return true;
        return path.equals("/health")
                || path.startsWith("/actuator/health")
                || path.startsWith("/api/docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
