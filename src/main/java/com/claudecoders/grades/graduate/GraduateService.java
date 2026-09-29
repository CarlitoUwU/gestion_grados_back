package com.claudecoders.grades.graduate;

import com.claudecoders.grades.graduate.dto.GraduateRequest;
import com.claudecoders.grades.graduate.dto.GraduateResponse;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GraduateService {
    private final GraduateRepository repository;
    private final EntityReferenceResolver refs;

    public GraduateService(GraduateRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<GraduateResponse> findAll() {
        return repository.findAll().stream().map(GraduateResponse::from).toList();
    }

    public GraduateResponse findById(Long id) {
        return GraduateResponse.from(get(id));
    }

    @Transactional
    public GraduateResponse create(GraduateRequest r) {
        return GraduateResponse.from(repository.save(toEntity(new Graduate(), r)));
    }

    @Transactional
    public GraduateResponse update(Long id, GraduateRequest r) {
        return GraduateResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private Graduate get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Graduate", id));
    }

    private Graduate toEntity(Graduate v, GraduateRequest r) {
        v.setDocumentNumber(blankNull(r.documentNumber()));
        v.setFirstNames(r.firstNames().trim());
        v.setLastNames(r.lastNames().trim());
        v.setSchool(refs.school(r.schoolId()));
        v.setAcademicProgram(blankNull(r.academicProgram()));
        return v;
    }

    private String blankNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
