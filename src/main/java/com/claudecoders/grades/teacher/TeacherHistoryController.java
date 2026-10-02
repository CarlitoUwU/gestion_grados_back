package com.claudecoders.grades.teacher;

import com.claudecoders.grades.teacher.dto.TeacherHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@Tag(name = "Teachers", description = "Teacher CRUD")
@SecurityRequirement(name = "bearer-jwt")
public class TeacherHistoryController {
    private final TeacherHistoryService service;

    public TeacherHistoryController(TeacherHistoryService service) {
        this.service = service;
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historial actual de participaciones del docente")
    public List<TeacherHistoryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }
}
