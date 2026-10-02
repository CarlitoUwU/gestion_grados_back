package com.claudecoders.grades.jurydrawmember;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryDrawMemberRepository extends JpaRepository<JuryDrawMember, Long> {
    boolean existsByJuryDrawIdAndTeacherId(Long juryDrawId, Long teacherId);

    List<JuryDrawMember> findByJuryDrawIdOrderById(Long juryDrawId);
}
