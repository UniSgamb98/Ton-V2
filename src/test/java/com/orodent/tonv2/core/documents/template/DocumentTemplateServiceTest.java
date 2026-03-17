package com.orodent.tonv2.core.documents.template;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(result.html().contains("<meta charset='UTF-8' />"));
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

    @Test
    void shouldRenderSingleTableWhenRowsComeFromEachBlock() {
        DocumentTemplateService service = new DocumentTemplateService();
        String template = "| Fase | Nome |\n" +
                "|------|------|\n" +
                "{{#each query.layers}}\n" +
                "| {{index}} | {{name}} |\n" +
                "{{/each}}";

        TemplateRenderResult result = service.render(template, Map.of(
                "query", Map.of("layers", java.util.List.of(
                        Map.of("name", "Base"),
                        Map.of("name", "Smalto")
                ))
        ));

        Matcher matcher = Pattern.compile("<table\\b").matcher(result.html());
        int tables = 0;
        while (matcher.find()) {
            tables++;
        }

        assertEquals(1, tables);
        assertTrue(result.html().contains("<td>1</td>"));
        assertTrue(result.html().contains("<td>2</td>"));
    }
}
