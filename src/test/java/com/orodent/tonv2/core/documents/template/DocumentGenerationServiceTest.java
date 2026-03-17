package com.orodent.tonv2.core.documents.template;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentGenerationServiceTest {

    @Test
    void shouldGenerateHtmlForBatchProduction() throws Exception {
        String dbName = "TonTemplateGenTestDb_" + System.nanoTime();
        Path outputDir = Files.createTempDirectory("ton-generated-");

        try (Connection conn = DriverManager.getConnection("jdbc:derby:" + dbName + ";create=true")) {
            TemplateStorageService storage = new TemplateStorageService(conn);
            TemplateStorageService.SavedTemplateRef saved = storage.saveTemplate(
                    "Scheda batch",
                    "Linea: {{line.name}}\nNote: {{notes}}\n{{#each items}}- {{code}} x {{quantity}}\n{{/each}}",
                    "{}"
            );

            DocumentGenerationService generationService = new DocumentGenerationService(
                    new DocumentTemplateService(),
                    storage,
                    outputDir
            );

            Path generated = generationService.generateForBatchProduction(
                    saved,
                    "Linea Demo",
                    "Note demo",
                    List.of(new DocumentGenerationService.BatchItemParam("ITEM-001", 12)),
                    99
            );

            assertTrue(Files.exists(generated));
            String html = Files.readString(generated);
            assertTrue(html.contains("Linea Demo"));
            assertTrue(html.contains("Note demo"));
            assertTrue(html.contains("ITEM-001"));
            assertTrue(html.contains("12"));
        }
    }
}
