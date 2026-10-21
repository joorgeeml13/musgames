package org.mus.shared.infrastructure.rest.response;

import java.time.Instant;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Instant timestamp;

    public ApiResponse() {
        this.success = true;
        this.timestamp = Instant.now();
    }

    public ApiResponse(T data, String message) {
        this.success = true;
        this.data = data;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public ApiResponse(boolean success, T data, String message, Instant timestamp) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, "OK");
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
