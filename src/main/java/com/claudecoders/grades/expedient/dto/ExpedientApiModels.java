package com.claudecoders.grades.expedient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ExpedientApiModels {
    private ExpedientApiModels() {}

    public record Summary(
            Long id,
            String number,
            LocalDate startDate,
            Long graduateId,
            String graduateName,
            String documentNumber,
            Long schoolId,
            String schoolName,
            String academicProgram,
            Long modalityId,
            String modalityName,
            Long statusId,
            String statusName,
            String researchTitle,
            LocalDate defenseDate,
            String defenseResult,
            LocalDateTime updatedAt,
            String degree,
            String observations,
            String studentCode,
            Long researchWorkId,
            String researchWorkType,
            String graduateFirstNames,
            String graduateLastNames,
            String createdByName,
            String updatedByName) {}

    public record Detail(
            Summary summary,
            List<ResolutionItem> resolutions,
            AdvisorItem advisor,
            List<JuryItem> jury,
            List<DrawItem> draws,
            DefenseItem defense,
            String degree,
            String observations,
            GraduateItem graduate,
            ResearchItem research) {}

    public record GraduateItem(
            Long id,
            String fullName,
            String documentNumber,
            String studentCode,
            Long schoolId,
            String schoolName,
            String academicProgram) {}

    public record ResearchItem(Long id, String title) {}

    public record PageResult(
            List<Summary> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {}

    public record ResolutionItem(
            Long id, String number, LocalDate date, String type, String fileUrl) {}

    public record AdvisorItem(
            Long id,
            Long teacherId,
            String teacherName,
            Long resolutionId,
            String resolutionNumber,
            LocalDate assignedAt) {}

    public record JuryItem(
            Long id,
            Long teacherId,
            String teacherName,
            String role,
            Long resolutionId,
            String resolutionNumber) {}

    public record DrawItem(
            Long id,
            LocalDate date,
            Long resolutionId,
            String resolutionNumber,
            List<DrawTeacherItem> teachers) {}

    public record DrawTeacherItem(Long teacherId, String teacherName) {}

    public record DefenseItem(Long id, LocalDate date, String result) {}
}
