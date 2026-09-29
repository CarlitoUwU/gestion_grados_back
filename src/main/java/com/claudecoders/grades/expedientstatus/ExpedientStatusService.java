package com.claudecoders.grades.expedientstatus;

import com.claudecoders.grades.expedientstatus.dto.ExpedientStatusRequest;
import com.claudecoders.grades.expedientstatus.dto.ExpedientStatusResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpedientStatusService {

    private final ExpedientStatusRepository repository;

    public ExpedientStatusService(ExpedientStatusRepository repository) {
        this.repository = repository;
    }

    public List<ExpedientStatusResponse> findAll() {
        return repository.findAll().stream().map(ExpedientStatusResponse::from).toList();
    }

    public ExpedientStatusResponse findById(Long id) {
        return ExpedientStatusResponse.from(getEntity(id));
    }

    @Transactional
    public ExpedientStatusResponse create(ExpedientStatusRequest request) {
        ensureNameAvailable(request.name());
        return ExpedientStatusResponse.from(
                repository.save(toEntity(new ExpedientStatus(), request)));
    }

    @Transactional
    public ExpedientStatusResponse update(Long id, ExpedientStatusRequest request) {
        ExpedientStatus status = getEntity(id);
        if (!status.getName().equalsIgnoreCase(request.name().trim()))
            ensureNameAvailable(request.name());
        return ExpedientStatusResponse.from(toEntity(status, request));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private ExpedientStatus getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpedientStatus", id));
    }

    private void ensureNameAvailable(String name) {
        if (repository.existsByNameIgnoreCase(name.trim()))
            throw new ConflictException("Ya existe un ExpedientStatus con ese nombre");
    }

    private ExpedientStatus toEntity(ExpedientStatus status, ExpedientStatusRequest request) {
        status.setName(request.name().trim());
        status.setActive(request.active());
        return status;
    }
}
