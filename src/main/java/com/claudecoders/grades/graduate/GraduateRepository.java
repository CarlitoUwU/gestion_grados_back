package com.claudecoders.grades.graduate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduateRepository extends JpaRepository<Graduate, Long> {
    Optional<Graduate> findFirstByDocumentNumberOrderByIdAsc(String documentNumber);
}
