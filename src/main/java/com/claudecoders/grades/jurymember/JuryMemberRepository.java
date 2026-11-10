package com.claudecoders.grades.jurymember;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JuryMemberRepository extends JpaRepository<JuryMember, Long> {
    @Query(
            """
            select j from JuryMember j where j.teacher.id = :teacherId
            and not exists (select newer.id from JuryMember newer where newer.expedient = j.expedient
                and newer.juryRole = j.juryRole and newer.id > j.id)
            order by j.id desc
            """)
    List<JuryMember> findCurrentByTeacherId(@Param("teacherId") Long teacherId);

    boolean existsByResolutionId(Long resolutionId);

    boolean existsByExpedientIdAndTeacherId(Long expedientId, Long teacherId);

    boolean existsByExpedientIdAndJuryRole(Long expedientId, JuryRole juryRole);

    List<JuryMember> findByExpedientIdOrderById(Long expedientId);

    List<JuryMember> findByTeacherIdOrderByIdDesc(Long teacherId);
}
