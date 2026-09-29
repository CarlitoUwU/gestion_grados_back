package com.claudecoders.grades.jurydrawmember;

import com.claudecoders.grades.jurydrawmember.dto.JuryDrawMemberRequest;
import com.claudecoders.grades.jurydrawmember.dto.JuryDrawMemberResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JuryDrawMemberService {
    private final JuryDrawMemberRepository repository;
    private final EntityReferenceResolver refs;

    public JuryDrawMemberService(
            JuryDrawMemberRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<JuryDrawMemberResponse> findAll() {
        return repository.findAll().stream().map(JuryDrawMemberResponse::from).toList();
    }

    public JuryDrawMemberResponse findById(Long id) {
        return JuryDrawMemberResponse.from(get(id));
    }

    @Transactional
    public JuryDrawMemberResponse create(JuryDrawMemberRequest r) {
        check(r, null);
        return JuryDrawMemberResponse.from(repository.save(toEntity(new JuryDrawMember(), r)));
    }

    @Transactional
    public JuryDrawMemberResponse update(Long id, JuryDrawMemberRequest r) {
        JuryDrawMember v = get(id);
        check(r, id);
        return JuryDrawMemberResponse.from(toEntity(v, r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private JuryDrawMember get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JuryDrawMember", id));
    }

    private void check(JuryDrawMemberRequest r, Long id) {
        if (repository.existsByJuryDrawIdAndTeacherId(r.juryDrawId(), r.teacherId())
                && (id == null
                        || !(get(id).getJuryDraw().getId().equals(r.juryDrawId())
                                && get(id).getTeacher().getId().equals(r.teacherId()))))
            throw new ConflictException("El docente ya está incluido en el sorteo");
    }

    private JuryDrawMember toEntity(JuryDrawMember v, JuryDrawMemberRequest r) {
        v.setJuryDraw(refs.juryDraw(r.juryDrawId()));
        v.setTeacher(refs.teacher(r.teacherId()));
        return v;
    }
}
