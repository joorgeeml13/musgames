package dev.ordago.shared.infrastructure.api.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ApiError {
    private boolean success;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private Instant timestamp;
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
