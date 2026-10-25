package edu.kuet.ums.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String what, Object id) {
            super(what + " not found: " + id);
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class FieldValidationException extends RuntimeException {
        private final java.util.Map<String, String> fieldErrors;

        public FieldValidationException(java.util.Map<String, String> fieldErrors) {
            super("Validation failed");
            this.fieldErrors = fieldErrors;
        }

        public java.util.Map<String, String> getFieldErrors() {
            return fieldErrors;
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}

