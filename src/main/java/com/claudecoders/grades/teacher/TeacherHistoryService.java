package com.claudecoders.grades.teacher;

import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.jurymember.JuryMemberRepository;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.teacher.dto.TeacherHistoryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherHistoryService {
    private final TeacherRepository teachers;
    private final ExpedientAdvisorRepository advisors;
    private final JuryMemberRepository juryMembers;
    private final ResearchWorkRepository researchWorks;

    public TeacherHistoryService(
            TeacherRepository teachers,
            ExpedientAdvisorRepository advisors,
            JuryMemberRepository juryMembers,
            ResearchWorkRepository researchWorks) {
        this.teachers = teachers;
        this.advisors = advisors;
        this.juryMembers = juryMembers;
        this.researchWorks = researchWorks;
    }

    public List<TeacherHistoryResponse> history(Long teacherId) {
        return history(teacherId, null);
    }

    public List<TeacherHistoryResponse> history(Long teacherId, Integer year) {
        if (!teachers.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher", teacherId);
        }

        List<TeacherHistoryResponse> history = new ArrayList<>();
        advisors.findCurrentByTeacherId(teacherId).stream()
                .filter(a -> inYear(year, a.getAssignedAt(), a.getCreatedAt()))
                .map(advisor -> row(advisor.getExpedient(), "Asesor", advisor.getResolution()))
                .forEach(history::add);
        juryMembers.findCurrentByTeacherId(teacherId).stream()
                .filter(
                        j ->
                                inYear(
                                        year,
                                        j.getResolution() == null
                                                ? null
                                                : j.getResolution().getResolutionDate(),
                                        j.getCreatedAt()))
                .map(
                        jury ->
                                row(
                                        jury.getExpedient(),
                                        "Jurado - " + jury.getJuryRole().name(),
                                        jury.getResolution()))
                .forEach(history::add);
        return history;
    }

    private static boolean inYear(Integer year, LocalDate assigned, LocalDateTime created) {
        if (year == null) return true;
        var date = assigned != null ? assigned : created == null ? null : created.toLocalDate();
        return date != null && date.getYear() == year;
    }

    private TeacherHistoryResponse row(
            Expedient expedient, String participation, Resolution resolution) {
        var graduate = expedient.getGraduate();
        var school = graduate.getSchool();
        var status = expedient.getStatus();
        var research = researchWorks.findByExpedientId(expedient.getId()).orElse(null);
        return new TeacherHistoryResponse(
                expedient.getId(),
                expedient.getNumber(),
                (graduate.getFirstNames() + " " + graduate.getLastNames()).trim(),
                research == null ? null : research.getTitle(),
                school == null ? null : school.getName(),
                participation,
                resolution == null ? null : resolution.getId(),
                resolution == null ? null : resolution.getNumber(),
                resolution == null ? null : resolution.getResolutionDate(),
                status == null ? null : status.getName());
    }
}
