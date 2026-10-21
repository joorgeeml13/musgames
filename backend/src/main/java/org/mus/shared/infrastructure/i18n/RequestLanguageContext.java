package org.mus.shared.infrastructure.i18n;

import jakarta.enterprise.context.RequestScoped;
import org.mus.shared.domain.i18n.Language;

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
