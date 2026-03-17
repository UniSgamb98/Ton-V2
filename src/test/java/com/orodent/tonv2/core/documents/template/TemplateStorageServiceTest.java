package com.orodent.tonv2.core.documents.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateStorageServiceTest {

    @Test
    void shouldSaveTemplateAsJsonFile() throws IOException {
        Path tempDir = Files.createTempDirectory("ton-templates-test-");
        TemplateStorageService storageService = new TemplateStorageService(tempDir);

        Path saved = storageService.saveTemplate(
                "Scheda Test",
                "**Titolo**",
                "{\"item\":{\"code\":\"A1\"}}"
        );

        assertTrue(Files.exists(saved));
        String content = Files.readString(saved);
        assertTrue(content.contains("\"templateName\": \"Scheda Test\""));
        assertTrue(content.contains("\"templateBody\": \"**Titolo**\""));
    }
}
