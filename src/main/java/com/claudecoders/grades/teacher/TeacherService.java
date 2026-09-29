package com.claudecoders.grades.teacher;

import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.teacher.dto.TeacherRequest;
import com.claudecoders.grades.teacher.dto.TeacherResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository repository;

    public TeacherService(TeacherRepository repository) {
        this.repository = repository;
    }

    public List<TeacherResponse> findAll() {
        return repository.findAll().stream().map(TeacherResponse::from).toList();
    }

    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getEntity(id));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        return TeacherResponse.from(repository.save(toEntity(new Teacher(), request)));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        return TeacherResponse.from(toEntity(getEntity(id), request));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Teacher getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }

    private Teacher toEntity(Teacher teacher, TeacherRequest request) {
        teacher.setDocumentNumber(trimToNull(request.documentNumber()));
        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(trimToNull(request.email()));
        return teacher;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
