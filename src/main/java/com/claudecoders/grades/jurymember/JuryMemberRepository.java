package com.claudecoders.grades.jurymember;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryMemberRepository extends JpaRepository<JuryMember, Long> {
    boolean existsByExpedientIdAndTeacherId(Long expedientId, Long teacherId);

    boolean existsByExpedientIdAndJuryRole(Long expedientId, JuryRole juryRole);

    List<JuryMember> findByExpedientIdOrderById(Long expedientId);

    List<JuryMember> findByTeacherIdOrderByIdDesc(Long teacherId);
}
