package com.claudecoders.grades.resolution;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    boolean existsByExpedientIdAndNumberIgnoreCase(Long expedientId, String number);

    List<Resolution> findByExpedientIdOrderByResolutionDateDescIdDesc(Long expedientId);
}
