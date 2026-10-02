package com.claudecoders.grades.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByGoogleSubjectAndIdNot(String googleSubject, Long id);

    java.util.Optional<User> findByEmailIgnoreCase(String email);

    java.util.Optional<User> findByGoogleSubject(String googleSubject);
}
