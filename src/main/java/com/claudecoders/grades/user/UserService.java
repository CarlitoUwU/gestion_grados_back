package com.claudecoders.grades.user;

import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.user.dto.UserRequest;
import com.claudecoders.grades.user.dto.UserResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
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

    private User get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void check(UserRequest r, Long id) {
        if (repository.existsByEmailIgnoreCase(r.email().trim()) && !sameEmail(r.email(), id))
            throw new ConflictException("Ya existe un User con ese email");
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
        v.setEmail(r.email().trim());
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
