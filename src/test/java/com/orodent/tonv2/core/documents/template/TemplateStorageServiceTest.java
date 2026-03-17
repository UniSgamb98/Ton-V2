package com.orodent.tonv2.core.documents.template;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateStorageServiceTest {

    @Test
    void shouldSaveTemplateInDatabase() throws Exception {
        String dbName = "TonTemplateTestDb_" + System.nanoTime();
        try (Connection conn = DriverManager.getConnection("jdbc:derby:" + dbName + ";create=true")) {
            TemplateStorageService storageService = new TemplateStorageService(conn);

            TemplateStorageService.SavedTemplateRef saved = storageService.saveTemplate(
                    "Scheda Test",
                    "**Titolo**",
                    "{\"item\":{\"code\":\"A1\"}}"
            );

            assertTrue(saved.id() > 0);
            assertFalse(storageService.listTemplates().isEmpty());
            TemplateStorageService.StoredTemplate loaded = storageService.loadTemplate(saved.id());
            assertTrue(loaded.templateName().contains("Scheda Test"));
            assertTrue(loaded.templateBody().contains("Titolo"));

            storageService.markTemplateAsUsed(saved.id());
            assertTrue(storageService.findLastUsedTemplate().isPresent());
            assertTrue(storageService.findLastUsedTemplate().get().id() == saved.id());
        }
    }
}
