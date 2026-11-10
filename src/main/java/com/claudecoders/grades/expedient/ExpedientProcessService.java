package com.claudecoders.grades.expedient;

import com.claudecoders.grades.defense.Defense;
import com.claudecoders.grades.defense.DefenseRepository;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedient.dto.ExpedientProcessRequests;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.jurydraw.JuryDraw;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.jurydrawmember.JuryDrawMember;
import com.claudecoders.grades.jurydrawmember.JuryDrawMemberRepository;
import com.claudecoders.grades.jurymember.JuryMember;
import com.claudecoders.grades.jurymember.JuryMemberRepository;
import com.claudecoders.grades.jurymember.JuryRole;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import com.claudecoders.grades.teacher.Teacher;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ExpedientProcessService {
    private final ExpedientService expedients;
    private final ExpedientQueryService queries;
    private final EntityReferenceResolver refs;
    private final ExpedientStatusRepository statuses;
    private final ResolutionRepository resolutions;
    private final ExpedientAdvisorRepository advisors;
    private final JuryMemberRepository jury;
    private final JuryDrawRepository draws;
    private final JuryDrawMemberRepository drawMembers;
    private final DefenseRepository defenses;

    public ExpedientProcessService(
            ExpedientService expedients,
            ExpedientQueryService queries,
            EntityReferenceResolver refs,
            ExpedientStatusRepository statuses,
            ResolutionRepository resolutions,
            ExpedientAdvisorRepository advisors,
            JuryMemberRepository jury,
            JuryDrawRepository draws,
            JuryDrawMemberRepository drawMembers,
            DefenseRepository defenses) {
        this.expedients = expedients;
        this.queries = queries;
        this.refs = refs;
        this.statuses = statuses;
        this.resolutions = resolutions;
        this.advisors = advisors;
        this.jury = jury;
        this.draws = draws;
        this.drawMembers = drawMembers;
        this.defenses = defenses;
    }

    public ExpedientApiModels.Detail status(
            String number, ExpedientProcessRequests.Status request, Authentication auth) {
        Expedient expedient = expedients.getForUpdate(number);
        var status =
                statuses.findByNameIgnoreCase(request.status().trim())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "ExpedientStatus", request.status()));
        if (!status.isActive()) throw badRequest("El estado está inactivo");
        expedient.setStatus(status);
        expedient.setObservations(
                request.observations() == null ? null : request.observations().trim());
        return result(expedient, auth);
    }

    public ExpedientApiModels.Detail advisor(
            String number, ExpedientProcessRequests.Advisor request, Authentication auth) {
        Expedient expedient = expedients.getForUpdate(number);
        Teacher teacher = refs.teacher(request.teacherId());
        Resolution resolution = resolution(expedient, request.resolutionNumber(), "ASESORIA");
        ExpedientAdvisor advisor =
                advisors.findFirstByExpedientIdOrderByIdDesc(expedient.getId())
                        .orElseGet(ExpedientAdvisor::new);
        advisor.setExpedient(expedient);
        advisor.setTeacher(teacher);
        advisor.setResolution(resolution);
        advisor.setAssignedAt(request.assignedAt());
        advisors.save(advisor);
        return result(expedient, auth);
    }

    public ExpedientApiModels.Detail jury(
            String number, ExpedientProcessRequests.Jury request, Authentication auth) {
        Expedient expedient = expedients.getForUpdate(number);
        Set<Long> teacherIds = new HashSet<>();
        Set<JuryRole> roles = new HashSet<>();
        List<Teacher> teachers =
                request.members().stream()
                        .map(
                                member -> {
                                    if (!teacherIds.add(member.teacherId()))
                                        throw badRequest(
                                                "Los docentes del jurado deben ser distintos");
                                    JuryRole role = role(member.role());
                                    if (!roles.add(role))
                                        throw badRequest(
                                                "Los cargos del jurado deben ser distintos");
                                    return refs.teacher(member.teacherId());
                                })
                        .toList();
        if (!roles.equals(Set.of(JuryRole.PRESIDENTE, JuryRole.SECRETARIO, JuryRole.VOCAL))) {
            throw badRequest("El jurado requiere Presidente, Secretario y Vocal");
        }
        Resolution resolution = resolution(expedient, request.resolutionNumber(), "JURADO");
        jury.deleteAll(jury.findByExpedientIdOrderById(expedient.getId()));
        jury.flush();
        for (int i = 0; i < request.members().size(); i++) {
            JuryMember member = new JuryMember();
            member.setExpedient(expedient);
            member.setTeacher(teachers.get(i));
            member.setJuryRole(role(request.members().get(i).role()));
            member.setResolution(resolution);
            jury.save(member);
        }
        return result(expedient, auth);
    }

    public ExpedientApiModels.Detail draw(
            String number, ExpedientProcessRequests.Draw request, Authentication auth) {
        Expedient expedient = expedients.getForUpdate(number);
        if (new HashSet<>(request.teacherIds()).size() != request.teacherIds().size()) {
            throw badRequest("Los docentes del sorteo deben ser distintos");
        }
        List<Teacher> teachers = request.teacherIds().stream().map(refs::teacher).toList();
        JuryDraw draw = new JuryDraw();
        draw.setExpedient(expedient);
        draw.setDrawDate(request.drawDate());
        draw.setResolution(resolution(expedient, request.resolutionNumber(), "SORTEO"));
        draws.save(draw);
        for (Teacher teacher : teachers) {
            JuryDrawMember member = new JuryDrawMember();
            member.setJuryDraw(draw);
            member.setTeacher(teacher);
            drawMembers.save(member);
        }
        return result(expedient, auth);
    }

    public ExpedientApiModels.Detail defense(
            String number, ExpedientProcessRequests.Defense request, Authentication auth) {
        Expedient expedient = expedients.getForUpdate(number);
        Defense defense = defenses.findByExpedientId(expedient.getId()).orElseGet(Defense::new);
        defense.setExpedient(expedient);
        defense.setDefenseDate(request.defenseDate());
        defense.setResult(request.result().trim());
        defenses.save(defense);
        return result(expedient, auth);
    }

    private Resolution resolution(Expedient expedient, String number, String type) {
        return resolutions
                .findByExpedientIdAndNumberIgnoreCase(expedient.getId(), number.trim())
                .orElseGet(
                        () -> {
                            Resolution value = new Resolution();
                            value.setExpedient(expedient);
                            value.setNumber(number.trim());
                            value.setResolutionType(type);
                            return resolutions.save(value);
                        });
    }

    private ExpedientApiModels.Detail result(Expedient expedient, Authentication auth) {
        expedients.touch(expedient, auth);
        return queries.detail(expedient.getId());
    }

    private static JuryRole role(String role) {
        try {
            return JuryRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw badRequest("Cargo de jurado inválido: " + role);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
