package com.claudecoders.grades.modality;

import com.claudecoders.grades.modality.dto.DegreeModalityRequest;
import com.claudecoders.grades.modality.dto.DegreeModalityResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DegreeModalityService {

    private final DegreeModalityRepository repository;

    public DegreeModalityService(DegreeModalityRepository repository) {
        this.repository = repository;
    }

    public List<DegreeModalityResponse> findAll() {
        return repository.findAll().stream().map(DegreeModalityResponse::from).toList();
    }

    public DegreeModalityResponse findById(Long id) {
        return DegreeModalityResponse.from(getEntity(id));
    }

    @Transactional
    public DegreeModalityResponse create(DegreeModalityRequest request) {
        ensureNameAvailable(request.name());
        return DegreeModalityResponse.from(
                repository.save(toEntity(new DegreeModality(), request)));
    }

    @Transactional
    public DegreeModalityResponse update(Long id, DegreeModalityRequest request) {
        DegreeModality modality = getEntity(id);
        if (!modality.getName().equalsIgnoreCase(request.name().trim()))
            ensureNameAvailable(request.name());
        return DegreeModalityResponse.from(toEntity(modality, request));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private DegreeModality getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DegreeModality", id));
    }

    private void ensureNameAvailable(String name) {
        if (repository.existsByNameIgnoreCase(name.trim()))
            throw new ConflictException("Ya existe una DegreeModality con ese nombre");
    }

    private DegreeModality toEntity(DegreeModality modality, DegreeModalityRequest request) {
        modality.setName(request.name().trim());
        modality.setActive(request.active());
        return modality;
    }
}
