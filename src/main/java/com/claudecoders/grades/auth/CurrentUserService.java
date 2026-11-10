package com.claudecoders.grades.auth;

import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public static final String DEFAULT_ROLE = "ADMIN_GT";

    private final UserRepository users;
    private final Set<String> allowedRoles;
    private final Set<String> allowedEmailDomains;

    public CurrentUserService(
            UserRepository users,
            @Value("${app.security.allowed-roles:ADMIN_GT}") String allowedRoles,
            @Value("${app.security.allowed-email-domains:unsa.edu.pe}")
                    String allowedEmailDomains) {
        this.users = users;
        this.allowedRoles = normalizedSet(allowedRoles, false);
        this.allowedEmailDomains = normalizedSet(allowedEmailDomains, true);
    }

    public User from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Se requiere una identidad JWT válida");
        }
        return find(authentication)
                .filter(user -> isAuthorized(user, jwt))
                .orElseThrow(
                        () ->
                                new AccessDeniedException(
                                        "El usuario no está registrado, activo o autorizado"));
    }

    public Optional<User> find(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            Optional<User> bySubject = users.findByGoogleSubject(subject.trim());
            if (bySubject.isPresent()) return bySubject;
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return users.findByEmailIgnoreCase(email.trim())
                    .filter(
                            user ->
                                    user.getGoogleSubject() == null
                                            || user.getGoogleSubject().isBlank());
        }

        return Optional.empty();
    }

    public boolean isAuthorized(User user, Jwt jwt) {
        if (!user.isActive()) return false;
        if (!allowedRoles.contains(normalize(user.getRole()))) return false;
        if (!isInstitutional(user.getEmail())) return false;

        String tokenEmail = jwt.getClaimAsString("email");
        if (tokenEmail != null && !tokenEmail.isBlank() && !isInstitutional(tokenEmail)) {
            return false;
        }
        if (Boolean.FALSE.equals(jwt.getClaim("email_verified"))) return false;

        String hostedDomain = jwt.getClaimAsString("hd");
        return hostedDomain == null
                || hostedDomain.isBlank()
                || allowedEmailDomains.contains(normalizeDomain(hostedDomain));
    }

    private boolean isInstitutional(String email) {
        if (email == null || email.isBlank()) return false;
        int at = email.lastIndexOf('@');
        return at >= 0
                && at < email.length() - 1
                && allowedEmailDomains.contains(normalizeDomain(email.substring(at + 1)));
    }

    private static Set<String> normalizedSet(String value, boolean domains) {
        return Arrays.stream(value == null ? new String[0] : value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .map(entry -> domains ? normalizeDomain(entry) : normalize(entry))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeDomain(String domain) {
        String normalized = normalize(domain);
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
