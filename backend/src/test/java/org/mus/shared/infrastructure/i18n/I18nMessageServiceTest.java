package org.mus.shared.infrastructure.i18n;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mus.shared.domain.i18n.I18nMessageService;
import org.mus.shared.domain.i18n.Language;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class I18nMessageServiceTest {

    @Inject
    I18nMessageService messageService;

    @Test
    void testMessageResolutionSpanish() {
        String msg = messageService.getMessage("system.health.ok", Language.ES);
        assertEquals("Servicio en funcionamiento", msg);
    }

    @Test
    void testMessageResolutionEnglish() {
        String msg = messageService.getMessage("system.health.ok", Language.EN);
        assertEquals("Service is healthy", msg);
    }

    @Test
    void testLanguageFromHeader() {
        assertEquals(Language.ES, Language.fromHeader(null));
        assertEquals(Language.ES, Language.fromHeader(""));
        assertEquals(Language.ES, Language.fromHeader("es"));
        assertEquals(Language.ES, Language.fromHeader("es-ES,es;q=0.9"));
        assertEquals(Language.EN, Language.fromHeader("en"));
        assertEquals(Language.EN, Language.fromHeader("en-US,en;q=0.8"));
        assertEquals(Language.ES, Language.fromHeader("fr"));
        // Excessively long header test (prevent memory DoS)
        String hugeHeader = "en,".repeat(500);
        assertEquals(Language.EN, Language.fromHeader(hugeHeader));
    }
}
