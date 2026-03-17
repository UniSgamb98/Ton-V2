package com.orodent.tonv2.core.documents.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentGenerationService {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DocumentTemplateService templateService;
    private final TemplateStorageService templateStorageService;
    private final Path outputDir;

    public DocumentGenerationService(DocumentTemplateService templateService,
                                     TemplateStorageService templateStorageService,
                                     Path outputDir) {
        this.templateService = templateService;
        this.templateStorageService = templateStorageService;
        this.outputDir = outputDir;
    }

    public Path generateForBatchProduction(TemplateStorageService.SavedTemplateRef templateRef,
                                           String productionLineName,
                                           String notes,
                                           List<BatchItemParam> batchItems,
                                           int productionOrderId) throws IOException {
        TemplateStorageService.StoredTemplate template = templateStorageService.loadTemplate(templateRef.id());
        templateStorageService.markTemplateAsUsed(templateRef.id());

        Map<String, Object> params = new HashMap<>(templateService.parseParameters(template.parametersJson()));
        List<Map<String, Object>> items = new ArrayList<>();
        for (BatchItemParam row : batchItems) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", row.code());
            item.put("quantity", row.quantity());
            items.add(item);
        }

        params.put("line", Map.of("name", productionLineName == null ? "" : productionLineName));
        params.put("notes", notes == null ? "" : notes);
        params.put("items", items);

        String html = templateService.render(template.templateBody(), params).html();

        Files.createDirectories(outputDir);
        String baseName = sanitizeFilePart(template.templateName());
        Path output = outputDir.resolve(baseName + "-order-" + productionOrderId + "-" + LocalDateTime.now().format(FILE_TS) + ".html");
        Files.writeString(output, html, StandardCharsets.UTF_8);
        return output;
    }

    private String sanitizeFilePart(String value) {
        String base = value == null ? "" : value.toLowerCase();
        String sanitized = base
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "documento-batch" : sanitized;
    }

    public record BatchItemParam(String code, int quantity) {}
}
