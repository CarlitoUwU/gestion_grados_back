package com.claudecoders.grades.health;

import com.claudecoders.grades.shared.exception.ApiError;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiError> root() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        new ApiError(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                "No existe un recurso en /. Usa /health o /api/docs.",
                                Map.of()));
    }
}
