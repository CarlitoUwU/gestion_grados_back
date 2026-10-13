package com.claudecoders.grades.expedient;

import com.claudecoders.grades.defense.Defense;
import com.claudecoders.grades.defense.DefenseRepository;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.jurydraw.JuryDraw;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.jurydrawmember.JuryDrawMemberRepository;
import com.claudecoders.grades.jurymember.JuryMember;
import com.claudecoders.grades.jurymember.JuryMemberRepository;
import com.claudecoders.grades.researchwork.ResearchWork;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpedientQueryService {
    private final ExpedientRepository expedients;
    private final ResearchWorkRepository researchWorks;
    private final ResolutionRepository resolutions;
    private final ExpedientAdvisorRepository advisors;
    private final JuryMemberRepository juryMembers;
    private final JuryDrawRepository draws;
    private final JuryDrawMemberRepository drawMembers;
    private final DefenseRepository defenses;

    public ExpedientQueryService(
            ExpedientRepository expedients,
            ResearchWorkRepository researchWorks,
            ResolutionRepository resolutions,
            ExpedientAdvisorRepository advisors,
            JuryMemberRepository juryMembers,
            JuryDrawRepository draws,
            JuryDrawMemberRepository drawMembers,
            DefenseRepository defenses) {
        this.expedients = expedients;
        this.researchWorks = researchWorks;
        this.resolutions = resolutions;
        this.advisors = advisors;
        this.juryMembers = juryMembers;
        this.draws = draws;
        this.drawMembers = drawMembers;
        this.defenses = defenses;
    }

    public List<ExpedientApiModels.Summary> search(
            String search, Long schoolId, Long statusId, Long modalityId, Integer year) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
        var found = expedients.search(normalizedSearch, schoolId, statusId, modalityId, year);
        if (found.isEmpty()) return List.of();
        var ids = found.stream().map(Expedient::getId).toList();
        Map<Long, ResearchWork> research =
                researchWorks.findByExpedientIdIn(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        r -> r.getExpedient().getId(), Function.identity()));
        Map<Long, Defense> defense =
                defenses.findByExpedientIdIn(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        d -> d.getExpedient().getId(), Function.identity()));
        return found.stream()
                .map(e -> summary(e, research.get(e.getId()), defense.get(e.getId())))
                .toList();
    }

    public ExpedientApiModels.PageResult search(
            String search,
            Long schoolId,
            Long statusId,
            Long modalityId,
            Integer year,
            int page,
            int size) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
        var result =
                expedients.search(
                        normalizedSearch,
                        schoolId,
                        statusId,
                        modalityId,
                        year,
                        org.springframework.data.domain.PageRequest.of(page, size));
        return new ExpedientApiModels.PageResult(
                result.getContent().stream().map(this::summary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    public ExpedientApiModels.Detail detail(String number) {
        var expedient =
                expedients
                        .findByNumberIgnoreCase(number.trim())
                        .orElseThrow(() -> new ResourceNotFoundException("Expedient", number));
        return detail(expedient.getId());
    }

    public ExpedientApiModels.Summary summary(Long id) {
        var expedient =
                expedients
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
        return summary(expedient);
    }

    public ExpedientApiModels.Detail detail(Long id) {
        Expedient expedient =
                expedients
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
        List<Resolution> resolutionItems =
                resolutions.findByExpedientIdOrderByResolutionDateDescIdDesc(id);
        var advisorItem = advisors.findFirstByExpedientIdOrderByIdDesc(id);
        List<JuryMember> juryItems = juryMembers.findByExpedientIdOrderById(id);
        List<JuryDraw> drawItems = draws.findByExpedientIdOrderByDrawDateDescIdDesc(id);
        Defense defense = defenses.findByExpedientId(id).orElse(null);
        var graduate = expedient.getGraduate();
        var school = graduate.getSchool();
        var research = researchWorks.findByExpedientId(id).orElse(null);

        return new ExpedientApiModels.Detail(
                summary(expedient),
                resolutionItems.stream().map(this::resolution).toList(),
                advisorItem.map(this::advisor).orElse(null),
                juryItems.stream().map(this::jury).toList(),
                drawItems.stream().map(this::draw).toList(),
                defense == null
                        ? null
                        : new ExpedientApiModels.DefenseItem(
                                defense.getId(), defense.getDefenseDate(), defense.getResult()),
                expedient.getDegree(),
                expedient.getObservations(),
                new ExpedientApiModels.GraduateItem(
                        graduate.getId(),
                        graduate.getFullName(),
                        graduate.getDocumentNumber(),
                        graduate.getStudentCode(),
                        school == null ? null : school.getId(),
                        school == null ? null : school.getName(),
                        graduate.getAcademicProgram()),
                research == null
                        ? null
                        : new ExpedientApiModels.ResearchItem(
                                research.getId(), research.getTitle()));
    }

    private ExpedientApiModels.Summary summary(Expedient expedient) {
        return summary(
                expedient,
                researchWorks.findByExpedientId(expedient.getId()).orElse(null),
                defenses.findByExpedientId(expedient.getId()).orElse(null));
    }

    private ExpedientApiModels.Summary summary(
            Expedient expedient, ResearchWork research, Defense defense) {
        var graduate = expedient.getGraduate();
        var school = graduate.getSchool();
        var modality = expedient.getModality();
        var status = expedient.getStatus();
        var createdBy = expedient.getCreatedBy();
        var updatedBy = expedient.getUpdatedBy();
        return new ExpedientApiModels.Summary(
                expedient.getId(),
                expedient.getNumber(),
                expedient.getStartDate(),
                graduate.getId(),
                graduate.getFullName(),
                graduate.getDocumentNumber(),
                school == null ? null : school.getId(),
                school == null ? null : school.getName(),
                graduate.getAcademicProgram(),
                modality == null ? null : modality.getId(),
                modality == null ? null : modality.getName(),
                status == null ? null : status.getId(),
                status == null ? null : status.getName(),
                research == null ? null : research.getTitle(),
                defense == null ? null : defense.getDefenseDate(),
                defense == null ? null : defense.getResult(),
                expedient.getUpdatedAt(),
                expedient.getDegree(),
                expedient.getObservations(),
                graduate.getStudentCode(),
                research == null ? null : research.getId(),
                research == null ? null : research.getWorkType(),
                graduate.getFirstNames(),
                graduate.getLastNames(),
                createdBy == null ? null : createdBy.getFullName(),
                updatedBy == null ? null : updatedBy.getFullName());
    }

    private ExpedientApiModels.ResolutionItem resolution(Resolution value) {
        return new ExpedientApiModels.ResolutionItem(
                value.getId(),
                value.getNumber(),
                value.getResolutionDate(),
                value.getResolutionType(),
                value.getFileUrl());
    }

    private ExpedientApiModels.AdvisorItem advisor(ExpedientAdvisor value) {
        var resolution = value.getResolution();
        return new ExpedientApiModels.AdvisorItem(
                value.getId(),
                value.getTeacher().getId(),
                value.getTeacher().getFullName(),
                resolution == null ? null : resolution.getId(),
                resolution == null ? null : resolution.getNumber(),
                value.getAssignedAt());
    }

    private ExpedientApiModels.JuryItem jury(JuryMember value) {
        var resolution = value.getResolution();
        return new ExpedientApiModels.JuryItem(
                value.getId(),
                value.getTeacher().getId(),
                value.getTeacher().getFullName(),
                value.getJuryRole().name(),
                resolution == null ? null : resolution.getId(),
                resolution == null ? null : resolution.getNumber());
    }

    private ExpedientApiModels.DrawItem draw(JuryDraw value) {
        var resolution = value.getResolution();
        return new ExpedientApiModels.DrawItem(
                value.getId(),
                value.getDrawDate(),
                resolution == null ? null : resolution.getId(),
                resolution == null ? null : resolution.getNumber(),
                drawMembers.findByJuryDrawIdOrderById(value.getId()).stream()
                        .map(
                                member ->
                                        new ExpedientApiModels.DrawTeacherItem(
                                                member.getTeacher().getId(),
                                                member.getTeacher().getFullName()))
                        .toList());
    }
}
