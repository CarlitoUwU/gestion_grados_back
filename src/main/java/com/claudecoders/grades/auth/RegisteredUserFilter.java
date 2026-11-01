package com.claudecoders.grades.auth;

import com.claudecoders.grades.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpMethod;
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
    private final JsonMapper mapper;

    public RegisteredUserFilter(CurrentUserService currentUser, JsonMapper mapper) {
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            var user = currentUser.find(authentication);
            if (user.isEmpty() || !currentUser.isAuthorized(user.get(), jwt)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(
                        response.getWriter(),
                        new ApiError(
                                Instant.now(),
                                403,
                                "Forbidden",
                                "El usuario no está registrado, activo o autorizado",
                                Map.of()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        return path.equals("/health")
                || path.startsWith("/actuator/health")
                || path.startsWith("/api/docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
