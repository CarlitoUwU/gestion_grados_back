package com.claudecoders.grades.defense;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefenseRepository extends JpaRepository<Defense, Long> {
    boolean existsByExpedientId(Long expedientId);

    java.util.Optional<Defense> findByExpedientId(Long expedientId);

    List<Defense> findByExpedientIdIn(Collection<Long> ids);
}
