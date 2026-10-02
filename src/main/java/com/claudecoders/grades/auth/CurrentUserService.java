package com.claudecoders.grades.auth;

import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public static final String DEFAULT_ROLE = "ADMIN_GT";

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User from(Authentication authentication) {
        return find(authentication)
                .orElseThrow(() -> new ResourceNotFoundException("AuthenticatedUser", 0L));
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
            return users.findByEmailIgnoreCase(email.trim());
        }

        return Optional.empty();
    }
}
