package com.claudecoders.grades.user;

import com.claudecoders.grades.shared.exception.BusinessValidationException;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.user.dto.UserRequest;
import com.claudecoders.grades.user.dto.UserResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final String allowedDomains;

    public UserService(
            UserRepository repository,
            @Value("${app.security.allowed-email-domains:unsa.edu.pe}") String allowedDomains) {
        this.repository = repository;
        this.allowedDomains = allowedDomains;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(get(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        check(request, null);
        return UserResponse.from(repository.save(toEntity(new User(), request)));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User value = get(id);
        check(request, id);
        return UserResponse.from(toEntity(value, request));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User value = get(id);
        value.setActive(active);
        return UserResponse.from(value);
    }

    private User get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void check(UserRequest r, Long id) {
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        String domain = email.substring(email.lastIndexOf('@') + 1);
        if (java.util.Arrays.stream(allowedDomains.split(","))
                .map(String::trim)
                .noneMatch(value -> value.replaceFirst("^@", "").equalsIgnoreCase(domain)))
            throw new BusinessValidationException(
                    "email", "Utiliza un correo institucional autorizado");
        if (repository.existsByEmailIgnoreCase(r.email().trim()) && !sameEmail(r.email(), id))
            throw new ConflictException("Ya existe un User con ese email");
        if (!"ADMIN_GT".equals(r.role().trim()))
            throw new ConflictException("El único rol permitido actualmente es ADMIN_GT");
        if (r.googleSubject() != null
                && !r.googleSubject().isBlank()
                && repository.existsByGoogleSubjectAndIdNot(
                        r.googleSubject().trim(), id == null ? 0L : id))
            throw new ConflictException("Ya existe un User con ese googleSubject");
    }

    private boolean sameEmail(String email, Long id) {
        return id != null && get(id).getEmail().equalsIgnoreCase(email.trim());
    }

    private User toEntity(User v, UserRequest r) {
        v.setEmail(r.email().trim().toLowerCase(Locale.ROOT));
        v.setFullName(r.fullName().trim());
        v.setGoogleSubject(blankNull(r.googleSubject()));
        v.setRole(r.role().trim());
        v.setActive(r.active());
        return v;
    }

    private String blankNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
