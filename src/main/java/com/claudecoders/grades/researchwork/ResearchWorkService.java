package com.claudecoders.grades.researchwork;

import com.claudecoders.grades.researchwork.dto.ResearchWorkRequest;
import com.claudecoders.grades.researchwork.dto.ResearchWorkResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResearchWorkService {
    private final ResearchWorkRepository repository;
    private final EntityReferenceResolver refs;

    public ResearchWorkService(ResearchWorkRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<ResearchWorkResponse> findAll() {
        return repository.findAll().stream().map(ResearchWorkResponse::from).toList();
    }

    public ResearchWorkResponse findById(Long id) {
        return ResearchWorkResponse.from(get(id));
    }

    @Transactional
    public ResearchWorkResponse create(ResearchWorkRequest r) {
        checkExpedient(r.expedientId(), null);
        return ResearchWorkResponse.from(repository.save(toEntity(new ResearchWork(), r)));
    }

    @Transactional
    public ResearchWorkResponse update(Long id, ResearchWorkRequest r) {
        checkExpedient(r.expedientId(), id);
        return ResearchWorkResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private ResearchWork get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchWork", id));
    }

    private void checkExpedient(Long expedientId, Long id) {
        if (repository.existsByExpedientId(expedientId)
                && (id == null || !get(id).getExpedient().getId().equals(expedientId)))
            throw new ConflictException("El expediente ya tiene un ResearchWork");
    }

    private ResearchWork toEntity(ResearchWork v, ResearchWorkRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setWorkType(blankNull(r.workType()));
        v.setTitle(r.title().trim());
        return v;
    }

    private String blankNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
