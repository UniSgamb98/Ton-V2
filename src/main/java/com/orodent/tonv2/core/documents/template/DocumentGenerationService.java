package com.orodent.tonv2.core.documents.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                                           Map<String, Object> params,
                                           int productionOrderId) throws IOException {
        TemplateStorageService.StoredTemplate template = templateStorageService.loadTemplate(templateRef.id());
        templateStorageService.markTemplateAsUsed(templateRef.id());

        Map<String, Object> mergedParams = new HashMap<>(templateService.parseParameters(template.parametersJson()));
        if (params != null) {
            mergedParams.putAll(params);
        }

        String html = templateService.render(template.templateBody(), mergedParams).html();

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
}
