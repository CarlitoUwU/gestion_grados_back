package com.claudecoders.grades.resolution;

public class DocumentTooLargeException extends RuntimeException {
    public DocumentTooLargeException() {
        super("El PDF supera el máximo de 10 MiB");
    }
}
