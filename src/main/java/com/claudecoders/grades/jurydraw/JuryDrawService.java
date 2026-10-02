package com.claudecoders.grades.jurydraw;

import com.claudecoders.grades.jurydraw.dto.JuryDrawRequest;
import com.claudecoders.grades.jurydraw.dto.JuryDrawResponse;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.time.LocalDate;
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
        checkBusinessRules(r);
        return JuryDrawResponse.from(repository.save(toEntity(new JuryDraw(), r)));
    }

    @Transactional
    public JuryDrawResponse update(Long id, JuryDrawRequest r) {
        checkBusinessRules(r);
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

    private void checkBusinessRules(JuryDrawRequest r) {
        var expedient = refs.expedient(r.expedientId());
        if (r.drawDate() != null && r.drawDate().isBefore(expedient.getStartDate()))
            throw new ConflictException(
                    "La fecha del sorteo no puede ser anterior al inicio del expediente");
        if (r.drawDate() != null && r.drawDate().isAfter(LocalDate.now()))
            throw new ConflictException("La fecha del sorteo no puede ser posterior a hoy");
    }

    private JuryDraw toEntity(JuryDraw v, JuryDrawRequest r) {
        v.setExpedient(refs.expedient(r.expedientId()));
        v.setResolution(refs.resolution(r.resolutionId()));
        v.setDrawDate(r.drawDate());
        return v;
    }
}
