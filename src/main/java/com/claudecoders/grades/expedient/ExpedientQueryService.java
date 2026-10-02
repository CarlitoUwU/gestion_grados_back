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
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.util.List;
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
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return expedients.search(normalizedSearch, schoolId, statusId, modalityId, year).stream()
                .map(this::summary)
                .toList();
    }

    public ExpedientApiModels.Detail detail(Long id) {
        Expedient expedient =
                expedients
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
        List<Resolution> resolutionItems =
                resolutions.findByExpedientIdOrderByResolutionDateDescIdDesc(id);
        List<ExpedientAdvisor> advisorItems =
                advisors.findByExpedientIdOrderByAssignedAtDescIdDesc(id);
        List<JuryMember> juryItems = juryMembers.findByExpedientIdOrderById(id);
        List<JuryDraw> drawItems = draws.findByExpedientIdOrderByDrawDateDescIdDesc(id);
        Defense defense = defenses.findByExpedientId(id).orElse(null);

        return new ExpedientApiModels.Detail(
                summary(expedient),
                resolutionItems.stream().map(this::resolution).toList(),
                advisorItems.stream().findFirst().map(this::advisor).orElse(null),
                juryItems.stream().map(this::jury).toList(),
                drawItems.stream().map(this::draw).toList(),
                defense == null
                        ? null
                        : new ExpedientApiModels.DefenseItem(
                                defense.getId(), defense.getDefenseDate(), defense.getResult()));
    }

    private ExpedientApiModels.Summary summary(Expedient expedient) {
        var graduate = expedient.getGraduate();
        var school = graduate.getSchool();
        var modality = expedient.getModality();
        var status = expedient.getStatus();
        var research = researchWorks.findByExpedientId(expedient.getId()).orElse(null);
        var defense = defenses.findByExpedientId(expedient.getId()).orElse(null);
        return new ExpedientApiModels.Summary(
                expedient.getId(),
                expedient.getNumber(),
                expedient.getStartDate(),
                graduate.getId(),
                (graduate.getFirstNames() + " " + graduate.getLastNames()).trim(),
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
                expedient.getUpdatedAt());
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
