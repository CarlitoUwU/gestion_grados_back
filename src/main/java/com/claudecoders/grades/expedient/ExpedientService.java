package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.ExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpedientService {
    private final ExpedientRepository repository;
    private final EntityReferenceResolver refs;

    public ExpedientService(ExpedientRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<ExpedientResponse> findAll() {
        return repository.findAll().stream().map(ExpedientResponse::from).toList();
    }

    public ExpedientResponse findById(Long id) {
        return ExpedientResponse.from(get(id));
    }

    @Transactional
    public ExpedientResponse create(ExpedientRequest r) {
        checkNumber(r.number(), null);
        return ExpedientResponse.from(repository.save(toEntity(new Expedient(), r)));
    }

    @Transactional
    public ExpedientResponse update(Long id, ExpedientRequest r) {
        Expedient v = get(id);
        if (!v.getNumber().equalsIgnoreCase(r.number().trim())) checkNumber(r.number(), id);
        return ExpedientResponse.from(toEntity(v, r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private Expedient get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
    }

    private void checkNumber(String n, Long id) {
        if (repository.existsByNumberIgnoreCase(n.trim())
                && (id == null || !get(id).getNumber().equalsIgnoreCase(n.trim())))
            throw new ConflictException("Ya existe un Expedient con ese número");
    }

    private Expedient toEntity(Expedient v, ExpedientRequest r) {
        v.setNumber(r.number().trim());
        v.setStartDate(r.startDate());
        v.setGraduate(refs.graduate(r.graduateId()));
        v.setModality(refs.modality(r.modalityId()));
        v.setStatus(refs.status(r.statusId()));
        v.setCreatedBy(refs.user(r.createdById()));
        v.setUpdatedBy(refs.user(r.updatedById()));
        return v;
    }
}
