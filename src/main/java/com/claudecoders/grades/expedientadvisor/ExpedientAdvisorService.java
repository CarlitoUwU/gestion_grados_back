package com.claudecoders.grades.expedientadvisor;

import com.claudecoders.grades.expedientadvisor.dto.ExpedientAdvisorRequest;
import com.claudecoders.grades.expedientadvisor.dto.ExpedientAdvisorResponse;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpedientAdvisorService {
    private final ExpedientAdvisorRepository repository;
    private final EntityReferenceResolver refs;

    public ExpedientAdvisorService(
            ExpedientAdvisorRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<ExpedientAdvisorResponse> findAll() {
        return repository.findAll().stream().map(ExpedientAdvisorResponse::from).toList();
    }

    public ExpedientAdvisorResponse findById(Long id) {
        return ExpedientAdvisorResponse.from(get(id));
    }

    @Transactional
    public ExpedientAdvisorResponse create(ExpedientAdvisorRequest r) {
        return ExpedientAdvisorResponse.from(repository.save(toEntity(new ExpedientAdvisor(), r)));
    }

    @Transactional
    public ExpedientAdvisorResponse update(Long id, ExpedientAdvisorRequest r) {
        return ExpedientAdvisorResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private ExpedientAdvisor get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpedientAdvisor", id));
    }

    private ExpedientAdvisor toEntity(ExpedientAdvisor v, ExpedientAdvisorRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setTeacher(refs.teacher(r.teacherId()));
        v.setResolution(refs.resolution(r.resolutionId()));
        v.setAssignedAt(r.assignedAt());
        return v;
    }
}
