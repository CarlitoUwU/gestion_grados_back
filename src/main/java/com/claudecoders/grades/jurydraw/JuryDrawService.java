package com.claudecoders.grades.jurydraw;

import com.claudecoders.grades.jurydraw.dto.JuryDrawRequest;
import com.claudecoders.grades.jurydraw.dto.JuryDrawResponse;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JuryDrawService {
    private final JuryDrawRepository repository;
    private final EntityReferenceResolver refs;

    public JuryDrawService(JuryDrawRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<JuryDrawResponse> findAll() {
        return repository.findAll().stream().map(JuryDrawResponse::from).toList();
    }

    public JuryDrawResponse findById(Long id) {
        return JuryDrawResponse.from(get(id));
    }

    @Transactional
    public JuryDrawResponse create(JuryDrawRequest r) {
        return JuryDrawResponse.from(repository.save(toEntity(new JuryDraw(), r)));
    }

    @Transactional
    public JuryDrawResponse update(Long id, JuryDrawRequest r) {
        return JuryDrawResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private JuryDraw get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JuryDraw", id));
    }

    private JuryDraw toEntity(JuryDraw v, JuryDrawRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setResolution(refs.resolution(r.resolutionId()));
        v.setDrawDate(r.drawDate());
        return v;
    }
}
