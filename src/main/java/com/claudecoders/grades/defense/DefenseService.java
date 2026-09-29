package com.claudecoders.grades.defense;

import com.claudecoders.grades.defense.dto.DefenseRequest;
import com.claudecoders.grades.defense.dto.DefenseResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefenseService {
    private final DefenseRepository repository;
    private final EntityReferenceResolver refs;

    public DefenseService(DefenseRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<DefenseResponse> findAll() {
        return repository.findAll().stream().map(DefenseResponse::from).toList();
    }

    public DefenseResponse findById(Long id) {
        return DefenseResponse.from(get(id));
    }

    @Transactional
    public DefenseResponse create(DefenseRequest r) {
        checkExpedient(r.expedientId(), null);
        return DefenseResponse.from(repository.save(toEntity(new Defense(), r)));
    }

    @Transactional
    public DefenseResponse update(Long id, DefenseRequest r) {
        checkExpedient(r.expedientId(), id);
        return DefenseResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private Defense get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Defense", id));
    }

    private void checkExpedient(Long expedientId, Long id) {
        if (repository.existsByExpedientId(expedientId)
                && (id == null || !get(id).getExpedient().getId().equals(expedientId)))
            throw new ConflictException("El expediente ya tiene una Defense");
    }

    private Defense toEntity(Defense v, DefenseRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setDefenseDate(r.defenseDate());
        v.setResult(r.result() == null || r.result().isBlank() ? null : r.result().trim());
        return v;
    }
}
