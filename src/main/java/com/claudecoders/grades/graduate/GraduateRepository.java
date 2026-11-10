package com.claudecoders.grades.graduate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduateRepository extends JpaRepository<Graduate, Long> {
    java.util.List<Graduate> findByDocumentNumber(String documentNumber);

    java.util.List<Graduate> findByStudentCode(String studentCode);

    Optional<Graduate> findFirstByDocumentNumberOrderByIdAsc(String documentNumber);
}
