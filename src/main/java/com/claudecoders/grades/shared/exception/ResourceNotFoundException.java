package com.claudecoders.grades.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(
                "Teacher".equals(resource)
                        ? "Docente no encontrado"
                        : resource + " con id " + id + " no encontrado");
    }
}
