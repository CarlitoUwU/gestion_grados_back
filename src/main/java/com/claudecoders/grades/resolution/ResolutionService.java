package com.claudecoders.grades.resolution;

import com.claudecoders.grades.resolution.dto.ResolutionRequest;
import com.claudecoders.grades.resolution.dto.ResolutionResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResolutionService {
    private final ResolutionRepository repository;
    private final EntityReferenceResolver refs;

    public ResolutionService(ResolutionRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<ResolutionResponse> findAll() {
        return repository.findAll().stream().map(ResolutionResponse::from).toList();
    }

    public ResolutionResponse findById(Long id) {
        return ResolutionResponse.from(get(id));
    }

    @Transactional
    public ResolutionResponse create(ResolutionRequest r) {
        check(r, null);
        return ResolutionResponse.from(repository.save(toEntity(new Resolution(), r)));
    }

    @Transactional
    public ResolutionResponse update(Long id, ResolutionRequest r) {
        Resolution v = get(id);
        check(r, id);
        return ResolutionResponse.from(toEntity(v, r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private Resolution get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resolution", id));
    }

    private void check(ResolutionRequest r, Long id) {
        if (repository.existsByExpedientIdAndNumberIgnoreCase(r.expedientId(), r.number().trim())
                && (id == null
                        || !(get(id).getExpedient().getId().equals(r.expedientId())
                                && get(id).getNumber().equalsIgnoreCase(r.number().trim()))))
            throw new ConflictException(
                    "Ya existe una Resolution con ese número para el expediente");
    }

    private Resolution toEntity(Resolution v, ResolutionRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setNumber(r.number().trim());
        v.setResolutionDate(r.resolutionDate());
        v.setResolutionType(blankNull(r.resolutionType()));
        v.setFileUrl(blankNull(r.fileUrl()));
        return v;
    }

    private String blankNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
