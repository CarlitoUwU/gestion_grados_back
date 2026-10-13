package com.claudecoders.grades.expedient;

import com.claudecoders.grades.auth.CurrentUserService;
import com.claudecoders.grades.defense.DefenseRepository;
import com.claudecoders.grades.expedient.dto.CompleteExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedient.dto.ExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientResponse;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.researchwork.ResearchWork;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.shared.exception.BusinessValidationException;
import com.claudecoders.grades.shared.exception.ConflictException;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import com.claudecoders.grades.user.User;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpedientService {
    private final ExpedientRepository repository;
    private final EntityReferenceResolver refs;
    private final CurrentUserService currentUser;
    private final ExpedientQueryService queries;
    private final GraduateRepository graduates;
    private final ResearchWorkRepository researchWorks;
    private final JuryDrawRepository draws;
    private final DefenseRepository defenses;
    private final Clock clock;

    public ExpedientService(
            ExpedientRepository repository,
            EntityReferenceResolver refs,
            CurrentUserService currentUser,
            ExpedientQueryService queries,
            GraduateRepository graduates,
            ResearchWorkRepository researchWorks,
            JuryDrawRepository draws,
            DefenseRepository defenses,
            Clock clock) {
        this.repository = repository;
        this.refs = refs;
        this.currentUser = currentUser;
        this.queries = queries;
        this.graduates = graduates;
        this.researchWorks = researchWorks;
        this.draws = draws;
        this.defenses = defenses;
        this.clock = clock;
    }

    public List<ExpedientResponse> findAll() {
        return repository.findAll().stream().map(ExpedientResponse::from).toList();
    }

    public ExpedientResponse findById(Long id) {
        return ExpedientResponse.from(get(id));
    }

    @Transactional
    public ExpedientResponse create(ExpedientRequest r, Authentication authentication) {
        checkStartDate(r.startDate());
        checkNumber(r.number(), null);
        var entity = toEntity(new Expedient(), r);
        entity.setCreatedBy(currentUser.from(authentication));
        entity.setUpdatedBy(entity.getCreatedBy());
        return ExpedientResponse.from(repository.save(entity));
    }

    @Transactional
    public ExpedientResponse update(Long id, ExpedientRequest r, Authentication authentication) {
        checkStartDate(r.startDate());
        Expedient v = get(id);
        if (!v.getNumber().equalsIgnoreCase(r.number().trim())) checkNumber(r.number(), id);
        toEntity(v, r);
        v.setUpdatedBy(currentUser.from(authentication));
        v.setUpdatedAt(java.time.LocalDateTime.now(clock));
        return ExpedientResponse.from(v);
    }

    @Transactional
    public ExpedientApiModels.Detail createComplete(
            CompleteExpedientRequest r, Authentication authentication) {
        User user = currentUser.from(authentication);
        checkNumber(r.number(), null);
        checkComplete(r);
        Graduate graduate = graduates.save(fillGraduate(new Graduate(), r.graduate()));
        Expedient value = new Expedient();
        value.setGraduate(graduate);
        fillComplete(value, r, user);
        value.setCreatedBy(user);
        value = repository.save(value);
        ResearchWork work = new ResearchWork();
        work.setExpedient(value);
        fillResearch(work, r.research());
        researchWorks.save(work);
        repository.flush();
        return queries.detail(value.getId());
    }

    @Transactional
    public ExpedientApiModels.Detail updateComplete(
            Long id, CompleteExpedientRequest r, Authentication authentication) {
        User user = currentUser.from(authentication);
        Expedient value = get(id);
        checkNumber(r.number(), id);
        checkComplete(r);
        Graduate proposed = fillGraduate(new Graduate(), r.graduate());
        Graduate current = value.getGraduate();
        if (repository.existsByGraduateIdAndIdNot(current.getId(), id)
                && (!Objects.equals(current.getFirstNames(), proposed.getFirstNames())
                        || !Objects.equals(current.getLastNames(), proposed.getLastNames())
                        || !Objects.equals(
                                current.getDocumentNumber(), proposed.getDocumentNumber())
                        || !Objects.equals(
                                current.getAcademicProgram(), proposed.getAcademicProgram())
                        || !Objects.equals(
                                current.getSchool() == null ? null : current.getSchool().getId(),
                                proposed.getSchool().getId())))
            throw new ConflictException("El graduando está vinculado a otros expedientes");
        if (draws.findByExpedientIdOrderByDrawDateDescIdDesc(id).stream()
                        .anyMatch(
                                d ->
                                        d.getDrawDate() != null
                                                && d.getDrawDate().isBefore(r.startDate()))
                || defenses.findByExpedientId(id).stream()
                        .anyMatch(
                                d ->
                                        d.getDefenseDate() != null
                                                && d.getDefenseDate().isBefore(r.startDate())))
            throw new BusinessValidationException(
                    "startDate", "Es posterior a un sorteo o sustentación registrado");
        fillGraduate(value.getGraduate(), r.graduate());
        fillComplete(value, r, user);
        ResearchWork work =
                researchWorks
                        .findByExpedientId(id)
                        .orElseGet(
                                () -> {
                                    ResearchWork created = new ResearchWork();
                                    created.setExpedient(value);
                                    return created;
                                });
        researchWorks.save(fillResearch(work, r.research()));
        repository.flush();
        return queries.detail(id);
    }

    @Transactional
    public ExpedientApiModels.Summary updateStatus(
            Long id, Long statusId, Authentication authentication) {
        var status = refs.status(statusId);
        if (status == null || !status.isActive())
            throw new BusinessValidationException(
                    "statusId", "El estado debe existir y estar activo");
        Expedient value = get(id);
        value.setStatus(status);
        value.setUpdatedBy(currentUser.from(authentication));
        value.setUpdatedAt(java.time.LocalDateTime.now(clock));
        repository.flush();
        return queries.summary(id);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private Expedient get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
    }

    private void checkNumber(String n, Long id) {
        if (repository.existsByNumberIgnoreCase(n.trim())
                && (id == null || !get(id).getNumber().equalsIgnoreCase(n.trim())))
            throw new ConflictException("Ya existe un Expedient con ese número");
    }

    private Expedient toEntity(Expedient v, ExpedientRequest r) {
        v.setNumber(r.number().trim());
        v.setStartDate(r.startDate());
        v.setGraduate(refs.graduate(r.graduateId()));
        v.setModality(refs.modality(r.modalityId()));
        v.setStatus(refs.status(r.statusId()));
        return v;
    }

    private void checkComplete(CompleteExpedientRequest r) {
        checkStartDate(r.startDate());
        if (!refs.modality(r.modalityId()).isActive())
            throw new BusinessValidationException("modalityId", "La modalidad debe estar activa");
        if (!refs.status(r.statusId()).isActive())
            throw new BusinessValidationException("statusId", "El estado debe estar activo");
        refs.school(r.graduate().schoolId());
        var g = r.graduate();
        if ((g.firstNames() == null || g.firstNames().isBlank())
                && (g.fullName() == null || g.fullName().isBlank()))
            throw new BusinessValidationException(
                    "graduate.firstNames", "Indique nombres o fullName");
        if (g.firstNames() != null
                && !g.firstNames().isBlank()
                && (g.lastNames() == null || g.lastNames().isBlank()))
            throw new BusinessValidationException(
                    "graduate.lastNames",
                    "Los apellidos son obligatorios al enviar nombres separados");
    }

    private void checkStartDate(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock)))
            throw new BusinessValidationException(
                    "startDate", "No puede ser posterior a hoy en Lima");
    }

    private Graduate fillGraduate(Graduate value, CompleteExpedientRequest.GraduateInput g) {
        String firstNames =
                g.firstNames() == null || g.firstNames().isBlank()
                        ? g.fullName().trim()
                        : g.firstNames().trim();
        if (firstNames.length() > 150)
            throw new BusinessValidationException("graduate.firstNames", "Supera 150 caracteres");
        value.setFirstNames(firstNames);
        value.setLastNames(g.lastNames() == null ? "" : g.lastNames().trim());
        value.setDocumentNumber(g.documentNumber() == null ? null : g.documentNumber().trim());
        value.setAcademicProgram(g.academicProgram() == null ? null : g.academicProgram().trim());
        value.setSchool(refs.school(g.schoolId()));
        return value;
    }

    private void fillComplete(Expedient value, CompleteExpedientRequest r, User user) {
        value.setNumber(r.number().trim().toUpperCase(Locale.ROOT));
        value.setStartDate(r.startDate());
        value.setModality(refs.modality(r.modalityId()));
        value.setStatus(refs.status(r.statusId()));
        value.setUpdatedBy(user);
        value.setUpdatedAt(java.time.LocalDateTime.now(clock));
    }

    private ResearchWork fillResearch(
            ResearchWork value, CompleteExpedientRequest.ResearchInput r) {
        value.setTitle(r.title().trim());
        value.setWorkType(r.workType() == null ? null : r.workType().trim());
        return value;
    }
}
