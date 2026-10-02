package com.claudecoders.grades.catalog;

import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.modality.DegreeModalityRepository;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.teacher.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalogs")
@Tag(name = "Catalogs", description = "Lookup lists for frontend selects, filters, and forms")
@SecurityRequirement(name = "bearer-jwt")
public class CatalogController {
    private final SchoolRepository schools;
    private final DegreeModalityRepository modalities;
    private final ExpedientStatusRepository statuses;
    private final TeacherRepository teachers;

    public CatalogController(
            SchoolRepository schools,
            DegreeModalityRepository modalities,
            ExpedientStatusRepository statuses,
            TeacherRepository teachers) {
        this.schools = schools;
        this.modalities = modalities;
        this.statuses = statuses;
        this.teachers = teachers;
    }

    @GetMapping("/schools")
    @Operation(summary = "Listar escuelas")
    public List<CatalogItemResponse> schools() {
        return schools.findAll().stream()
                .map(school -> new CatalogItemResponse(school.getId(), school.getName(), true))
                .toList();
    }

    @GetMapping("/degree-modalities")
    @Operation(summary = "Listar modalidades de grado o título")
    public List<CatalogItemResponse> modalities() {
        return modalities.findAll().stream()
                .map(
                        modality ->
                                new CatalogItemResponse(
                                        modality.getId(), modality.getName(), modality.isActive()))
                .toList();
    }

    @GetMapping("/expedient-statuses")
    @Operation(summary = "Listar estados de expediente")
    public List<CatalogItemResponse> statuses() {
        return statuses.findAll().stream()
                .map(
                        status ->
                                new CatalogItemResponse(
                                        status.getId(), status.getName(), status.isActive()))
                .toList();
    }

    @GetMapping("/teachers")
    @Operation(summary = "Listar docentes para asignaciones")
    public List<CatalogTeacherResponse> teachers() {
        return teachers.findAll().stream()
                .map(
                        teacher ->
                                new CatalogTeacherResponse(
                                        teacher.getId(),
                                        teacher.getFullName(),
                                        teacher.getEmail(),
                                        teacher.getDocumentNumber()))
                .toList();
    }
}
