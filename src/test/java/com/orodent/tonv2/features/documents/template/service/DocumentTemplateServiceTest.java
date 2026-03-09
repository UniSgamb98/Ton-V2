package com.orodent.tonv2.features.documents.template.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTemplateServiceTest {

    @Test
    void shouldRenderBoldHrAndTable() {
        DocumentTemplateService service = new DocumentTemplateService();
        String template = "**Titolo**\n---\n| Col | Val |\n|-----|-----|\n| A | {{item.code}} |";

        TemplateRenderResult result = service.render(template, Map.of("item", Map.of("code", "ART-1")));

        assertTrue(result.html().contains("<strong>Titolo</strong>"));
        assertTrue(result.html().contains("<hr />"));
        assertTrue(result.html().contains("<table"));
        assertTrue(result.html().contains("ART-1"));
    }

    @Test
    void shouldExpandEachBlock() {
        DocumentTemplateService service = new DocumentTemplateService();
        String template = "{{#each query.layers}}| {{index}} | {{name}} |\\n{{/each}}";

        TemplateRenderResult result = service.render(template, Map.of(
                "query", Map.of("layers", java.util.List.of(
                        Map.of("name", "Base"),
                        Map.of("name", "Smalto")
                ))
        ));

        assertTrue(result.resolvedMarkup().contains("| 1 | Base |"));
        assertTrue(result.resolvedMarkup().contains("| 2 | Smalto |"));
    }
}
