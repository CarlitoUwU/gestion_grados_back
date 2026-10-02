package com.claudecoders.grades.jurymember;

import com.claudecoders.grades.jurymember.dto.JuryMemberRequest;
import com.claudecoders.grades.jurymember.dto.JuryMemberResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JuryMemberService {
    private final JuryMemberRepository repository;
    private final EntityReferenceResolver refs;

    public JuryMemberService(JuryMemberRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<JuryMemberResponse> findAll() {
        return repository.findAll().stream().map(JuryMemberResponse::from).toList();
    }

    public JuryMemberResponse findById(Long id) {
        return JuryMemberResponse.from(get(id));
    }

    @Transactional
    public JuryMemberResponse create(JuryMemberRequest r) {
        check(r, null);
        return JuryMemberResponse.from(repository.save(toEntity(new JuryMember(), r)));
    }

    @Transactional
    public JuryMemberResponse update(Long id, JuryMemberRequest r) {
        JuryMember v = get(id);
        check(r, id);
        return JuryMemberResponse.from(toEntity(v, r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private JuryMember get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JuryMember", id));
    }

    private void check(JuryMemberRequest r, Long id) {
        JuryMember current = id == null ? null : get(id);
        if (repository.existsByExpedientIdAndTeacherId(r.expedientId(), r.teacherId())
                && (current == null
                        || !(current.getExpedient().getId().equals(r.expedientId())
                                && current.getTeacher().getId().equals(r.teacherId()))))
            throw new ConflictException("El docente ya pertenece al jurado del expediente");
        if (repository.existsByExpedientIdAndJuryRole(r.expedientId(), r.juryRole())
                && (current == null
                        || !(current.getExpedient().getId().equals(r.expedientId())
                                && current.getJuryRole().equals(r.juryRole()))))
            throw new ConflictException("El cargo de jurado ya está asignado en el expediente");
    }

    private JuryMember toEntity(JuryMember v, JuryMemberRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setTeacher(refs.teacher(r.teacherId()));
        v.setResolution(refs.resolution(r.resolutionId()));
        v.setJuryRole(r.juryRole());
        return v;
    }
}
