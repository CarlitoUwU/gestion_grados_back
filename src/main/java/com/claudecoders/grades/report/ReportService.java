package com.claudecoders.grades.report;

import com.claudecoders.grades.expedient.ExpedientQueryService;
import com.claudecoders.grades.teacher.TeacherRepository;
import com.claudecoders.grades.teacher.TeacherHistoryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ExpedientQueryService expedients;
    private final TeacherHistoryService teacherHistory;
    private final TeacherRepository teachers;

    public ReportService(
            ExpedientQueryService expedients,
            TeacherHistoryService teacherHistory,
            TeacherRepository teachers) {
        this.expedients = expedients;
        this.teacherHistory = teacherHistory;
        this.teachers = teachers;
    }

    public ReportResponse statistics(Integer year, Long schoolId, Long modalityId) {
        var records = expedients.search(null, schoolId, null, modalityId, year);
        return new ReportResponse(
                "Estadísticas para acreditación",
                List.of("Expediente", "Graduando", "Escuela", "Modalidad", "Estado", "Inicio"),
                records.stream()
                        .map(
                                r ->
                                        java.util.Arrays.<Object>asList(
                                                r.number(),
                                                r.graduateName(),
                                                value(r.schoolName()),
                                                value(r.modalityName()),
                                                value(r.statusName()),
                                                r.startDate()))
                        .toList());
    }

    public ReportResponse teachers(Long teacherId) {
        var history =
                teacherId == null
                        ? teachers.findAll().stream()
                                .flatMap(teacher -> teacherHistory.history(teacher.getId()).stream())
                                .toList()
                        : teacherHistory.history(teacherId);
        return new ReportResponse(
                "Asesores y jurados",
                List.of(
                        "Expediente",
                        "Graduando",
                        "Trabajo",
                        "Escuela",
                        "Participación",
                        "Resolución",
                        "Fecha resolución"),
                history.stream()
                        .map(
                                h ->
                                        java.util.Arrays.<Object>asList(
                                                h.expedientNumber(),
                                                h.graduateName(),
                                                value(h.researchTitle()),
                                                value(h.schoolName()),
                                                h.participation(),
                                                value(h.resolutionNumber()),
                                                h.resolutionDate()))
                        .toList());
    }

    public ReportResponse defendedWorks(Integer year, Long schoolId, Long modalityId) {
        var records =
                expedients.search(null, schoolId, null, modalityId, null).stream()
                        .filter(r -> r.defenseDate() != null)
                        .filter(r -> year == null || r.defenseDate().getYear() == year)
                        .filter(r -> !"Pendiente".equalsIgnoreCase(value(r.defenseResult())))
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
                                                                        detail.summary().graduateName(),
                                                                        value(
                                                                                detail.summary()
                                                                                        .schoolName()),
                                                                        draw.teachers().stream()
                                                                                .map(
                                                                                        teacher ->
                                                                                                teacher
                                                                                                        .teacherName())
                                                                                .collect(
                                                                                        java.util.stream
                                                                                                .Collectors
                                                                                                .joining("; ")),
                                                                        value(
                                                                                draw
                                                                                        .resolutionNumber()))))
                        .toList());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
