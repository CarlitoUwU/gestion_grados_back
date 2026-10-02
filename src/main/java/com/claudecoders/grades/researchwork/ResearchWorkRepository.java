package com.claudecoders.grades.researchwork;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchWorkRepository extends JpaRepository<ResearchWork, Long> {
    boolean existsByExpedientId(Long expedientId);

    java.util.Optional<ResearchWork> findByExpedientId(Long expedientId);
}
