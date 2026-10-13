package com.claudecoders.grades.expedient;

import com.claudecoders.grades.auth.CurrentUserService;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedient.dto.ExpedientWriteRequest;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.researchwork.ResearchWork;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ExpedientService {
    private final ExpedientRepository repository;
    private final EntityReferenceResolver refs;
    private final GraduateRepository graduates;
    private final ResearchWorkRepository researchWorks;
    private final ExpedientQueryService queries;
    private final CurrentUserService currentUser;

    public ExpedientService(
            ExpedientRepository repository,
            EntityReferenceResolver refs,
            GraduateRepository graduates,
            ResearchWorkRepository researchWorks,
            ExpedientQueryService queries,
            CurrentUserService currentUser) {
        this.repository = repository;
        this.refs = refs;
        this.graduates = graduates;
        this.researchWorks = researchWorks;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @Transactional
    public ExpedientApiModels.Detail create(ExpedientWriteRequest request, Authentication auth) {
        checkNumber(request.number(), null);
        Expedient expedient = new Expedient();
        expedient.setCreatedBy(currentUser.find(auth).orElse(null));
        apply(expedient, request, auth);
        repository.saveAndFlush(expedient);
        saveResearch(expedient, request.research());
        return queries.detail(expedient.getId());
    }

    @Transactional
    public ExpedientApiModels.Detail update(
            String number, ExpedientWriteRequest request, Authentication auth) {
        Expedient expedient = getForUpdate(number);
        checkNumber(request.number(), expedient);
        apply(expedient, request, auth);
        saveResearch(expedient, request.research());
        repository.flush();
        return queries.detail(expedient.getId());
    }

    @Transactional
    public void delete(String number) {
        repository.delete(getForUpdate(number));
        repository.flush();
    }

    Expedient getForUpdate(String number) {
        return repository
                .findForUpdateByNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Expedient", number));
    }

    void touch(Expedient expedient, Authentication auth) {
        currentUser.find(auth).ifPresent(expedient::setUpdatedBy);
        expedient.setUpdatedAt(LocalDateTime.now());
    }

    private void checkNumber(String number, Expedient current) {
        if (current != null && current.getNumber().equalsIgnoreCase(number.trim())) return;
        if (repository.existsByNumberIgnoreCase(number.trim())) {
            throw new ConflictException("Ya existe un expediente con ese número");
        }
    }

    private void apply(Expedient expedient, ExpedientWriteRequest request, Authentication auth) {
        var modality = refs.modality(request.modalityId());
        var status = refs.status(request.statusId());
        if (modality != null && !modality.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La modalidad está inactiva");
        }
        if (status != null && !status.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado está inactivo");
        }
        expedient.setNumber(request.number().trim());
        expedient.setStartDate(request.startDate());
        expedient.setDegree(blankNull(request.degree()));
        expedient.setObservations(blankNull(request.observations()));
        expedient.setModality(modality);
        expedient.setStatus(status);
        expedient.setGraduate(saveGraduate(request.graduate(), expedient.getGraduate()));
        touch(expedient, auth);
    }

    private Graduate saveGraduate(ExpedientWriteRequest.GraduateInput request, Graduate current) {
        String document = blankNull(request.documentNumber());
        String studentCode = blankNull(request.studentCode());
        Map<Long, Graduate> matches = new LinkedHashMap<>();
        if (document != null) {
            graduates.findByDocumentNumber(document).forEach(g -> matches.put(g.getId(), g));
        }
        if (studentCode != null) {
            graduates.findByStudentCode(studentCode).forEach(g -> matches.put(g.getId(), g));
        }
        if (matches.size() > 1) {
            throw new ConflictException(
                    "Los identificadores del graduando coinciden con varios registros");
        }
        Graduate matched = matches.values().stream().findFirst().orElse(null);
        if (current != null && matched != null && !current.getId().equals(matched.getId())) {
            throw new ConflictException("El documento o código pertenece a otro graduando");
        }
        Graduate graduate = current != null ? current : matched != null ? matched : new Graduate();
        String fullName = request.fullName().trim();
        // Preserve known name parts when unchanged; never guess a new division of surnames.
        if (!fullName.equals(graduate.getFullName())) {
            graduate.setFirstNames("");
            graduate.setLastNames("");
        }
        graduate.setFullName(fullName);
        graduate.setDocumentNumber(document);
        graduate.setStudentCode(studentCode);
        graduate.setSchool(refs.school(request.schoolId()));
        graduate.setAcademicProgram(blankNull(request.academicProgram()));
        return graduates.save(graduate);
    }

    private void saveResearch(Expedient expedient, ExpedientWriteRequest.ResearchInput request) {
        if (request == null) return;
        ResearchWork research =
                researchWorks.findByExpedientId(expedient.getId()).orElseGet(ResearchWork::new);
        research.setExpedient(expedient);
        research.setTitle(request.title().trim());
        researchWorks.save(research);
    }

    private static String blankNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
