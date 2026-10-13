package com.claudecoders.grades.expedient;

import com.claudecoders.grades.auth.CurrentUserService;
import com.claudecoders.grades.defense.Defense;
import com.claudecoders.grades.defense.DefenseRepository;
import com.claudecoders.grades.expedient.dto.AcademicRequest;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.jurydraw.JuryDraw;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.jurydrawmember.JuryDrawMember;
import com.claudecoders.grades.jurydrawmember.JuryDrawMemberRepository;
import com.claudecoders.grades.jurymember.JuryMember;
import com.claudecoders.grades.jurymember.JuryMemberRepository;
import com.claudecoders.grades.jurymember.JuryRole;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.shared.exception.BusinessValidationException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpedientAcademicService {
    private final EntityReferenceResolver refs;
    private final ExpedientRepository expedients;
    private final ResolutionRepository resolutions;
    private final ExpedientAdvisorRepository advisors;
    private final JuryMemberRepository juries;
    private final JuryDrawRepository draws;
    private final JuryDrawMemberRepository drawMembers;
    private final DefenseRepository defenses;
    private final ExpedientQueryService queries;
    private final CurrentUserService users;
    private final Clock clock;

    public ExpedientAcademicService(
            EntityReferenceResolver refs,
            ExpedientRepository expedients,
            ResolutionRepository resolutions,
            ExpedientAdvisorRepository advisors,
            JuryMemberRepository juries,
            JuryDrawRepository draws,
            JuryDrawMemberRepository drawMembers,
            DefenseRepository defenses,
            ExpedientQueryService queries,
            CurrentUserService users,
            Clock clock) {
        this.refs = refs;
        this.expedients = expedients;
        this.resolutions = resolutions;
        this.advisors = advisors;
        this.juries = juries;
        this.draws = draws;
        this.drawMembers = drawMembers;
        this.defenses = defenses;
        this.queries = queries;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public ExpedientApiModels.Detail replace(
            Long id, AcademicRequest request, Authentication authentication) {
        var expedient = refs.expedient(id);
        var user = users.from(authentication);
        var oldResolutions = resolutions.findByExpedientIdOrderByResolutionDateDescIdDesc(id);
        var oldAdvisors = advisors.findByExpedientIdOrderByAssignedAtDescIdDesc(id);
        var oldJury = juries.findByExpedientIdOrderById(id);
        var oldDraws = draws.findByExpedientIdOrderByDrawDateDescIdDesc(id);
        Map<Long, Resolution> existingResolutions =
                oldResolutions.stream()
                        .collect(java.util.stream.Collectors.toMap(Resolution::getId, r -> r));
        Map<Long, JuryDraw> existingDraws =
                oldDraws.stream()
                        .collect(java.util.stream.Collectors.toMap(JuryDraw::getId, d -> d));
        Map<Long, JuryMember> existingJury =
                oldJury.stream()
                        .collect(java.util.stream.Collectors.toMap(JuryMember::getId, j -> j));
        Map<Long, ExpedientAdvisor> existingAdvisors =
                oldAdvisors.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(ExpedientAdvisor::getId, a -> a));

        // Check the entire request before changing persistent entities.
        Set<Long> keptResolutions = new HashSet<>();
        Set<String> numbers = new HashSet<>();
        Set<String> clientIds = new HashSet<>();
        for (int i = 0; i < request.resolutions().size(); i++) {
            var r = request.resolutions().get(i);
            String path = "resolutions[" + i + "]";
            if (r.id() != null
                    && (!existingResolutions.containsKey(r.id()) || !keptResolutions.add(r.id())))
                invalid(path + ".id", "ID repetido o ajeno al expediente");
            if (!numbers.add(r.number().trim().toUpperCase(Locale.ROOT)))
                invalid(path + ".number", "Número de resolución duplicado");
            if (r.clientId() != null && (r.clientId().isBlank() || !clientIds.add(r.clientId())))
                invalid(path + ".clientId", "clientId vacío o duplicado");
        }
        Set<Long> keptAdvisors = new HashSet<>();
        if (request.advisor() != null) {
            var a = request.advisor();
            refs.teacher(a.teacherId());
            if (a.id() != null
                    && (!existingAdvisors.containsKey(a.id()) || !keptAdvisors.add(a.id())))
                invalid("advisor.id", "ID ajeno al expediente");
            validateClientId(a.resolutionClientId(), clientIds, "advisor.resolutionClientId");
        }
        Set<Long> keptJury = new HashSet<>();
        Set<Long> teachers = new HashSet<>();
        Set<JuryRole> roles = new HashSet<>();
        List<AcademicRequest.JuryInput> jury = request.jury() == null ? List.of() : request.jury();
        if (request.jury() != null && jury.isEmpty())
            invalid("jury", "Indique al menos un miembro o use null para eliminar el jurado");
        for (int i = 0; i < jury.size(); i++) {
            var j = jury.get(i);
            String path = "jury[" + i + "]";
            refs.teacher(j.teacherId());
            if (j.id() != null && (!existingJury.containsKey(j.id()) || !keptJury.add(j.id())))
                invalid(path + ".id", "ID repetido o ajeno al expediente");
            if (!teachers.add(j.teacherId())) invalid(path + ".teacherId", "Docente repetido");
            JuryRole role;
            try {
                role = JuryRole.valueOf(j.role());
            } catch (IllegalArgumentException exception) {
                throw new BusinessValidationException(path + ".role", "Cargo no admitido");
            }
            if (!roles.add(role)) invalid(path + ".role", "Cargo repetido");
            validateClientId(j.resolutionClientId(), clientIds, path + ".resolutionClientId");
        }
        Set<Long> keptDraws = new HashSet<>();
        for (int i = 0; i < request.draws().size(); i++) {
            var d = request.draws().get(i);
            String path = "draws[" + i + "]";
            if (d.id() != null && (!existingDraws.containsKey(d.id()) || !keptDraws.add(d.id())))
                invalid(path + ".id", "ID repetido o ajeno al expediente");
            if (d.date().isBefore(expedient.getStartDate())
                    || d.date().isAfter(LocalDate.now(clock)))
                invalid(path + ".date", "Debe estar entre el inicio y hoy en Lima");
            validateClientId(d.resolutionClientId(), clientIds, path + ".resolutionClientId");
            Set<Long> drawTeachers = new HashSet<>();
            for (Long teacherId : d.teacherIds()) {
                refs.teacher(teacherId);
                if (!drawTeachers.add(teacherId)) invalid(path + ".teacherIds", "Docente repetido");
            }
        }
        if (request.defense() != null) {
            var d = request.defense();
            if (d.date().isBefore(expedient.getStartDate()))
                invalid("defense.date", "No puede ser anterior al inicio");
            if (d.date().isAfter(LocalDate.now(clock))
                    && d.result() != null
                    && !d.result().isBlank()
                    && !"Pendiente".equalsIgnoreCase(d.result().trim()))
                invalid("defense.result", "Una sustentación futura solo puede estar Pendiente");
        }

        // Unlink dependents first, then delete omitted rows before reusing unique values.
        oldAdvisors.stream()
                .filter(a -> !keptAdvisors.contains(a.getId()))
                .forEach(advisors::delete);
        oldJury.stream().filter(j -> !keptJury.contains(j.getId())).forEach(juries::delete);
        for (JuryDraw d : oldDraws)
            if (!keptDraws.contains(d.getId())) {
                drawMembers.deleteAll(drawMembers.findByJuryDrawIdOrderById(d.getId()));
                draws.delete(d);
            }
        advisors.flush();
        juries.flush();
        draws.flush();
        for (var a : oldAdvisors) if (keptAdvisors.contains(a.getId())) a.setResolution(null);
        for (var j : oldJury) if (keptJury.contains(j.getId())) j.setResolution(null);
        for (var d : oldDraws) if (keptDraws.contains(d.getId())) d.setResolution(null);
        advisors.flush();
        juries.flush();
        draws.flush();
        for (Resolution r : oldResolutions)
            if (!keptResolutions.contains(r.getId())) resolutions.delete(r);
        resolutions.flush();
        // Temporary numbers allow two preserved resolutions to exchange unique numbers.
        for (Resolution r : oldResolutions)
            if (keptResolutions.contains(r.getId()))
                r.setNumber("__REPLACING_" + UUID.randomUUID());
        resolutions.flush();

        Map<String, Resolution> resolved = new HashMap<>();
        for (var r : request.resolutions()) {
            Resolution value = r.id() == null ? new Resolution() : existingResolutions.get(r.id());
            value.setExpedient(expedient);
            value.setNumber(r.number().trim());
            value.setResolutionDate(r.date());
            value.setResolutionType(r.type() == null ? null : r.type().trim());
            value = resolutions.save(value); // fileUrl is untouched on preserved records.
            if (r.clientId() != null) resolved.put(r.clientId(), value);
        }
        resolutions.flush();
        if (request.advisor() != null) {
            var a = request.advisor();
            var value = a.id() == null ? new ExpedientAdvisor() : existingAdvisors.get(a.id());
            value.setExpedient(expedient);
            value.setTeacher(refs.teacher(a.teacherId()));
            value.setResolution(resolved.get(a.resolutionClientId()));
            value.setAssignedAt(a.assignedAt());
            advisors.save(value);
        }
        // Ceiling: swapping teachers between two preserved jury IDs can hit the immediate
        // UNIQUE(expedient_id, teacher_id). To support swaps, add a staged reassignment strategy.
        for (var j : jury) {
            var value = j.id() == null ? new JuryMember() : existingJury.get(j.id());
            value.setExpedient(expedient);
            value.setTeacher(refs.teacher(j.teacherId()));
            value.setJuryRole(JuryRole.valueOf(j.role()));
            value.setResolution(resolved.get(j.resolutionClientId()));
            juries.save(value);
        }
        for (var d : request.draws()) {
            var value = d.id() == null ? new JuryDraw() : existingDraws.get(d.id());
            value.setExpedient(expedient);
            value.setDrawDate(d.date());
            value.setResolution(resolved.get(d.resolutionClientId()));
            value = draws.save(value);
            if (d.id() != null) {
                drawMembers.deleteAll(drawMembers.findByJuryDrawIdOrderById(value.getId()));
                drawMembers.flush();
            }
            for (Long teacherId : d.teacherIds()) {
                var member = new JuryDrawMember();
                member.setJuryDraw(value);
                member.setTeacher(refs.teacher(teacherId));
                drawMembers.save(member);
            }
        }
        var oldDefense = defenses.findByExpedientId(id).orElse(null);
        if (request.defense() == null) {
            if (oldDefense != null) defenses.delete(oldDefense);
        } else {
            Defense value = oldDefense == null ? new Defense() : oldDefense;
            value.setExpedient(expedient);
            value.setDefenseDate(request.defense().date());
            value.setResult(
                    request.defense().result() == null ? null : request.defense().result().trim());
            defenses.save(value);
        }
        expedient.setUpdatedBy(user);
        expedient.setUpdatedAt(LocalDateTime.now(clock));
        expedients.flush();
        return queries.detail(id);
    }

    private static void validateClientId(String id, Set<String> ids, String field) {
        if (id != null && !ids.contains(id)) invalid(field, "resolutionClientId desconocido");
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException(field, message);
    }
}
