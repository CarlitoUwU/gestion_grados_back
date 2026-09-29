package com.claudecoders.grades.resolution;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    boolean existsByExpedientIdAndNumberIgnoreCase(Long expedientId, String number);
}
