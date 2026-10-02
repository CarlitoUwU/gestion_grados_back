package com.claudecoders.grades.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RegisteredUserFilter extends OncePerRequestFilter {
    private final CurrentUserService currentUser;
    private final Environment environment;

    public RegisteredUserFilter(CurrentUserService currentUser, Environment environment) {
        this.currentUser = currentUser;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            var user = currentUser.find(authentication);
            if (user.isEmpty() || !user.get().isActive()) {
                response.sendError(
                        HttpStatus.FORBIDDEN.value(),
                        "El usuario autenticado no está registrado o activo");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) return true;
        String path = request.getRequestURI();
        return path.equals("/health")
                || path.startsWith("/actuator/health")
                || path.startsWith("/api/docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
