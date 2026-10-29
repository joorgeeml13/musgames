package dev.ordago.shared.domain.i18n;

import java.util.Locale;

public enum Language {
    ES("es", Locale.of("es", "ES")),
    EN("en", Locale.of("en", "US"));

    private final String code;
    private final Locale locale;

    Language(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }

    public String getCode() {
        return code;
    }

    public Locale getLocale() {
        return locale;
    }

    public static Language fromHeader(String header) {
        if (header == null || header.isBlank()) {
            return ES;
        }
        // Security: Prevent memory consumption DoS by bounding header inspection length
        String boundedHeader = header.length() > 256 ? header.substring(0, 256) : header;
        String lower = boundedHeader.toLowerCase(Locale.ROOT);
        String[] parts = lower.split(",");
        for (String part : parts) {
            String tag = part.split(";")[0].trim();
            if (tag.startsWith("en")) {
                return EN;
            }
            if (tag.startsWith("es")) {
                return ES;
            }
        }
        return ES;
    }
}
