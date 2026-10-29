package dev.ordago.shared.domain.exception;

import java.util.ArrayList;
import java.util.List;

public class DomainException extends RuntimeException {
    private final String code;
    private final int statusCode;
    private final Object[] args;
    private final List<String> details;

    public DomainException(String code, String messageKey, int statusCode, List<String> details, Object... args) {
        super(messageKey);
        this.code = code;
        this.statusCode = statusCode;
        this.details = details != null ? details : new ArrayList<>();
        this.args = args != null ? args : new Object[0];
    }

    public DomainException(String code, String messageKey, int statusCode, Object... args) {
        this(code, messageKey, statusCode, List.of(), args);
    }

    public DomainException(String code, String messageKey) {
        this(code, messageKey, 400, List.of());
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object[] getArgs() {
        return args;
    }

    public List<String> getDetails() {
        return details;
    }
}
