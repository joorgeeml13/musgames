package dev.ordago.shared.infrastructure.api.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Standard error response payload")
public class ApiError {

    @Schema(description = "Success flag, always false for errors", example = "false")
    private boolean success;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error reason phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Application-specific error code", example = "VALIDATION_FAILED")
    private String code;

    @Schema(description = "Error description message", example = "Invalid request payload")
    private String message;

    @Schema(description = "Request URI path", example = "/api/v1/auth/login")
    private String path;

    @Schema(description = "Timestamp when the error occurred", example = "2026-10-29T15:30:00Z")
    private Instant timestamp;

    @Schema(description = "Detailed validation or contextual error items", example = "[\"email: Email must be a valid email address\"]")
    private List<String> details;

    public ApiError() {
        this.success = false;
        this.timestamp = Instant.now();
        this.details = new ArrayList<>();
    }

    public ApiError(int status, String error, String code, String message, String path, List<String> details) {
        this.success = false;
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        this.details = details != null ? details : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
