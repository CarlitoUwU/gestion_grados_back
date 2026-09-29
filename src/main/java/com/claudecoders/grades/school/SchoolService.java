package com.claudecoders.grades.school;

import com.claudecoders.grades.school.dto.SchoolRequest;
import com.claudecoders.grades.school.dto.SchoolResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SchoolService {

    private final SchoolRepository repository;

    public SchoolService(SchoolRepository repository) {
        this.repository = repository;
    }

    public List<SchoolResponse> findAll() {
        return repository.findAll().stream().map(SchoolResponse::from).toList();
    }

    public SchoolResponse findById(Long id) {
        return SchoolResponse.from(getEntity(id));
    }

    @Transactional
    public SchoolResponse create(SchoolRequest request) {
        ensureNameAvailable(request.name());
        School school = new School();
        school.setName(request.name().trim());
        return SchoolResponse.from(repository.save(school));
    }

    @Transactional
    public SchoolResponse update(Long id, SchoolRequest request) {
        School school = getEntity(id);
        if (!school.getName().equalsIgnoreCase(request.name().trim())) {
            ensureNameAvailable(request.name());
        }
        school.setName(request.name().trim());
        return SchoolResponse.from(school);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private School getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School", id));
    }

    private void ensureNameAvailable(String name) {
        if (repository.existsByNameIgnoreCase(name.trim())) {
            throw new ConflictException("Ya existe una School con ese nombre");
        }
    }
}
