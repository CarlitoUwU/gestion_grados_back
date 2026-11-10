package com.claudecoders.grades.jurydraw;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryDrawRepository extends JpaRepository<JuryDraw, Long> {
    boolean existsByResolutionId(Long resolutionId);

    List<JuryDraw> findByExpedientIdOrderByDrawDateDescIdDesc(Long expedientId);
}
