package org.mus.shared.infrastructure.i18n;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mus.shared.domain.i18n.I18nMessageService;
import org.mus.shared.domain.i18n.Language;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@ApplicationScoped
public class ResourceBundleMessageService implements I18nMessageService {

    private static final String BUNDLE_BASE_NAME = "messages";

    @Inject
    RequestLanguageContext requestLanguageContext;

    @Override
    public String getMessage(String key, Object... args) {
        Language currentLanguage = resolveCurrentLanguage();
        return getMessage(key, currentLanguage, args);
    }

    @Override
    public String getMessage(String key, Language language, Object... args) {
        if (key == null) {
            return "";
        }

        Language targetLang = language != null ? language : Language.ES;
        Locale locale = targetLang.getLocale();

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
            if (bundle.containsKey(key)) {
                String pattern = bundle.getString(key);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
        }

        // Fallback: try default bundle
        try {
            ResourceBundle defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Language.ES.getLocale());
            if (defaultBundle.containsKey(key)) {
                String pattern = defaultBundle.getString(key);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
        }

        // Final fallback: if arguments exist and key itself looks like a format pattern, format it; else return key
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(key, args);
            } catch (Exception ignored) {
            }
        }
        return key;
    }

    private Language resolveCurrentLanguage() {
        try {
            if (Arc.container().requestContext().isActive() && requestLanguageContext != null) {
                return requestLanguageContext.getLanguage();
            }
        } catch (Exception ignored) {
        }
        return Language.ES;
    }
}
