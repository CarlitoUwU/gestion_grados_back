package com.claudecoders.grades.resolution;

import com.claudecoders.grades.auth.CurrentUserService;
import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.jurymember.JuryMemberRepository;
import com.claudecoders.grades.resolution.dto.ResolutionRequest;
import com.claudecoders.grades.resolution.dto.ResolutionResponse;
import com.claudecoders.grades.shared.exception.BusinessValidationException;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ResolutionService {
    private final ResolutionRepository repository;
    private final EntityReferenceResolver refs;
    private final CurrentUserService currentUser;
    private final ExpedientAdvisorRepository advisors;
    private final JuryMemberRepository juryMembers;
    private final JuryDrawRepository draws;
    private final Clock clock;

    public ResolutionService(
            ResolutionRepository repository,
            EntityReferenceResolver refs,
            CurrentUserService currentUser,
            ExpedientAdvisorRepository advisors,
            JuryMemberRepository juryMembers,
            JuryDrawRepository draws,
            Clock clock) {
        this.repository = repository;
        this.refs = refs;
        this.currentUser = currentUser;
        this.advisors = advisors;
        this.juryMembers = juryMembers;
        this.draws = draws;
        this.clock = clock;
    }

    public List<ResolutionResponse> findAll() {
        return repository.findAll().stream().map(ResolutionResponse::from).toList();
    }

    public ResolutionResponse findById(Long id) {
        return ResolutionResponse.from(get(id));
    }

    @Transactional
    public ResolutionResponse create(ResolutionRequest r, Authentication authentication) {
        check(r, null);
        Expedient expedient = refs.expedient(r.expedientId());
        Resolution saved = repository.save(toEntity(new Resolution(), r, expedient));
        touch(expedient, authentication);
        repository.flush();
        return ResolutionResponse.from(saved);
    }

    @Transactional
    public ResolutionResponse update(Long id, ResolutionRequest r, Authentication authentication) {
        Resolution value = get(id);
        check(r, id);
        Expedient previous = value.getExpedient();
        Expedient target = refs.expedient(r.expedientId());
        boolean moved = !previous.getId().equals(target.getId());
        if (moved && isReferenced(id))
            throw new ConflictException(
                    "No se puede mover una resolución referenciada a otro expediente");
        toEntity(value, r, target);
        touch(previous, authentication);
        if (moved) touch(target, authentication);
        repository.flush();
        return ResolutionResponse.from(value);
    }

    @Transactional
    public ResolutionResponse uploadDocument(
            Long id, MultipartFile file, String downloadUrl, Authentication authentication)
            throws IOException {
        Resolution value = get(id);
        if (file == null
                || file.isEmpty()
                || !"application/pdf".equalsIgnoreCase(file.getContentType()))
            throw new BusinessValidationException(
                    "file", "Debe proporcionar un archivo application/pdf");
        if (file.getSize() > 10L * 1024 * 1024) throw new DocumentTooLargeException();
        byte[] data = file.getBytes();
        if (data.length < 5
                || data[0] != '%'
                || data[1] != 'P'
                || data[2] != 'D'
                || data[3] != 'F'
                || data[4] != '-')
            throw new BusinessValidationException("file", "El contenido no corresponde a un PDF");
        value.setDocumentData(data);
        value.setFileUrl(downloadUrl);
        touch(value.getExpedient(), authentication);
        repository.flush();
        return ResolutionResponse.from(value);
    }

    public byte[] document(Long id) {
        byte[] data = get(id).getDocumentData();
        if (data == null) throw new ResourceNotFoundException("Documento de resolución", id);
        return data;
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Resolution value = get(id);
        if (isReferenced(id))
            throw new ConflictException("La resolución está en uso y no puede eliminarse");
        Expedient expedient = value.getExpedient();
        repository.delete(value);
        touch(expedient, authentication);
        repository.flush();
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

    private boolean isReferenced(Long id) {
        return advisors.existsByResolutionId(id)
                || juryMembers.existsByResolutionId(id)
                || draws.existsByResolutionId(id);
    }

    private void touch(Expedient expedient, Authentication authentication) {
        expedient.setUpdatedBy(currentUser.from(authentication));
        expedient.setUpdatedAt(LocalDateTime.now(clock));
    }

    private Resolution toEntity(Resolution v, ResolutionRequest r, Expedient expedient) {
        v.setExpedient(expedient);
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
