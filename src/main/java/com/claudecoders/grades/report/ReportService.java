package com.claudecoders.grades.report;

import com.claudecoders.grades.expedient.ExpedientQueryService;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.teacher.TeacherHistoryService;
import com.claudecoders.grades.teacher.TeacherRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ExpedientQueryService expedients;
    private final TeacherHistoryService teacherHistory;
    private final TeacherRepository teachers;
    private final Clock clock;

    public ReportService(
            ExpedientQueryService expedients,
            TeacherHistoryService teacherHistory,
            TeacherRepository teachers,
            Clock clock) {
        this.expedients = expedients;
        this.teacherHistory = teacherHistory;
        this.teachers = teachers;
        this.clock = clock;
    }

    public ReportResponse statistics(Integer year, Long schoolId, Long modalityId) {
        var records = expedients.search(null, schoolId, null, modalityId, year);
        return new ReportResponse(
                "Estadísticas para acreditación",
                List.of(
                        "Expediente",
                        "Graduando",
                        "Escuela",
                        "Modalidad",
                        "Estado",
                        "Inicio",
                        "Sustentación",
                        "Resultado"),
                records.stream()
                        .map(
                                r ->
                                        java.util.Arrays.<Object>asList(
                                                r.number(),
                                                r.graduateName(),
                                                value(r.schoolName()),
                                                value(r.modalityName()),
                                                value(r.statusName()),
                                                r.startDate(),
                                                r.defenseDate(),
                                                value(r.defenseResult())))
                        .toList());
    }

    public ReportResponse teachers(Long teacherId, Integer year) {
        var selected =
                teacherId == null
                        ? teachers.findAll()
                        : List.of(
                                teachers.findById(teacherId)
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Teacher", teacherId)));
        var rows =
                selected.stream()
                        .flatMap(
                                teacher ->
                                        teacherHistory.history(teacher.getId(), year).stream()
                                                .map(
                                                        h ->
                                                                java.util.Arrays.<Object>asList(
                                                                        h.expedientNumber(),
                                                                        h.graduateName(),
                                                                        value(h.researchTitle()),
                                                                        value(h.schoolName()),
                                                                        h.participation(),
                                                                        value(h.resolutionNumber()),
                                                                        h.resolutionDate(),
                                                                        teacher.getId(),
                                                                        value(
                                                                                teacher
                                                                                        .getFullName()))))
                        .toList();
        return new ReportResponse(
                "Asesores y jurados",
                List.of(
                        "Expediente",
                        "Graduando",
                        "Trabajo",
                        "Escuela",
                        "Participación",
                        "Resolución",
                        "Fecha resolución",
                        "ID docente",
                        "Docente"),
                rows);
    }

    public ReportResponse defendedWorks(Integer year, Long schoolId, Long modalityId) {
        LocalDate asOfDate = LocalDate.now(clock);
        var records =
                expedients.search(null, schoolId, null, modalityId, null).stream()
                        .filter(r -> r.defenseDate() != null)
                        .filter(r -> !r.defenseDate().isAfter(asOfDate))
                        .filter(r -> year == null || r.defenseDate().getYear() == year)
                        .filter(r -> isDefended(r.defenseResult()))
                        .toList();
        return new ReportResponse(
                "Trabajos sustentados",
                List.of(
                        "Escuela",
                        "Año",
                        "Expediente",
                        "Graduando",
                        "Trabajo",
                        "Modalidad",
                        "Fecha",
                        "Resultado"),
                records.stream()
                        .map(
                                r ->
                                        java.util.Arrays.<Object>asList(
                                                value(r.schoolName()),
                                                r.defenseDate().getYear(),
                                                r.number(),
                                                r.graduateName(),
                                                value(r.researchTitle()),
                                                value(r.modalityName()),
                                                r.defenseDate(),
                                                value(r.defenseResult())))
                        .toList());
    }

    public ReportResponse draws(Integer year, Long schoolId) {
        var details =
                expedients.search(null, schoolId, null, null, null).stream()
                        .map(summary -> expedients.detail(summary.id()))
                        .toList();
        return new ReportResponse(
                "Sorteos externos",
                List.of("Fecha", "Expediente", "Graduando", "Escuela", "Docentes", "Resolución"),
                details.stream()
                        .flatMap(
                                detail ->
                                        detail.draws().stream()
                                                .filter(
                                                        draw ->
                                                                year == null
                                                                        || (draw.date() != null
                                                                                && draw.date()
                                                                                                .getYear()
                                                                                        == year))
                                                .map(
                                                        draw ->
                                                                java.util.Arrays.<Object>asList(
                                                                        draw.date(),
                                                                        detail.summary().number(),
                                                                        detail.summary()
                                                                                .graduateName(),
                                                                        value(
                                                                                detail.summary()
                                                                                        .schoolName()),
                                                                        draw.teachers().stream()
                                                                                .map(
                                                                                        teacher ->
                                                                                                teacher
                                                                                                        .teacherName())
                                                                                .collect(
                                                                                        java.util
                                                                                                .stream
                                                                                                .Collectors
                                                                                                .joining(
                                                                                                        "; ")),
                                                                        value(
                                                                                draw
                                                                                        .resolutionNumber()))))
                        .toList());
    }

    private static boolean isDefended(String result) {
        if (result == null || result.isBlank()) return false;
        var normalized =
                Normalizer.normalize(result.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "");
        return !"pendiente".equals(normalized);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
