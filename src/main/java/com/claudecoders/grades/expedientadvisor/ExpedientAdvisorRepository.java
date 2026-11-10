package com.claudecoders.grades.expedientadvisor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpedientAdvisorRepository extends JpaRepository<ExpedientAdvisor, Long> {
    java.util.Optional<ExpedientAdvisor> findFirstByExpedientIdOrderByIdDesc(Long expedientId);

    @Query(
            """
            select a from ExpedientAdvisor a where a.teacher.id = :teacherId
            and not exists (select b.id from ExpedientAdvisor b where b.expedient = a.expedient
                and (coalesce(b.assignedAt, cast(b.createdAt as LocalDate)) > coalesce(a.assignedAt, cast(a.createdAt as LocalDate))
                or (coalesce(b.assignedAt, cast(b.createdAt as LocalDate)) = coalesce(a.assignedAt, cast(a.createdAt as LocalDate)) and b.id > a.id)))
            order by a.id desc
            """)
    List<ExpedientAdvisor> findCurrentByTeacherId(@Param("teacherId") Long teacherId);

    boolean existsByResolutionId(Long resolutionId);

    List<ExpedientAdvisor> findByExpedientIdOrderByAssignedAtDescIdDesc(Long expedientId);

    List<ExpedientAdvisor> findByTeacherIdOrderByAssignedAtDescIdDesc(Long teacherId);
}
