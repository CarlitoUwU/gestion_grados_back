package com.claudecoders.grades.defense;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DefenseRepository extends JpaRepository<Defense, Long> {
    boolean existsByExpedientId(Long expedientId);
}
