package dev.ordago.shared.infrastructure.api.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import dev.ordago.shared.domain.i18n.Language;
import dev.ordago.shared.infrastructure.i18n.RequestLanguageContext;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 100)
public class LanguageFilter implements ContainerRequestFilter {

    @Inject
    RequestLanguageContext languageContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String acceptLanguage = requestContext.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
        Language lang = Language.fromHeader(acceptLanguage);
        languageContext.setLanguage(lang);
        requestContext.setProperty("request.language", lang);
    }
}
