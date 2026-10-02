package com.claudecoders.grades.expedientadvisor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpedientAdvisorRepository extends JpaRepository<ExpedientAdvisor, Long> {
    List<ExpedientAdvisor> findByExpedientIdOrderByAssignedAtDescIdDesc(Long expedientId);

    List<ExpedientAdvisor> findByTeacherIdOrderByAssignedAtDescIdDesc(Long teacherId);
}
