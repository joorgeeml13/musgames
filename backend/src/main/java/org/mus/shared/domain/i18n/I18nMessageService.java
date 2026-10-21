package org.mus.shared.domain.i18n;

public interface I18nMessageService {
    String getMessage(String key, Object... args);
    String getMessage(String key, Language language, Object... args);
}
