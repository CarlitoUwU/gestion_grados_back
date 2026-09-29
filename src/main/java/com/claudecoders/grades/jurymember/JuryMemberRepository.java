package com.claudecoders.grades.jurymember;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryMemberRepository extends JpaRepository<JuryMember, Long> {
    boolean existsByExpedientIdAndTeacherId(Long expedientId, Long teacherId);
}
