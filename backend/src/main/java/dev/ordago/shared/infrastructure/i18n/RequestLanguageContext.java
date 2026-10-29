package dev.ordago.shared.infrastructure.i18n;

import jakarta.enterprise.context.RequestScoped;
import dev.ordago.shared.domain.i18n.Language;

@RequestScoped
public class RequestLanguageContext {
    private Language language = Language.ES;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}
