package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateStorageService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path storageDir;
    private final Gson gson;

    public TemplateStorageService(Path storageDir) {
        this.storageDir = storageDir;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Path saveTemplate(String templateName, String templateBody, String parametersJson, String htmlOutput) throws IOException {
        Files.createDirectories(storageDir);

        String safeName = sanitizeName(templateName);
        String timestamp = LocalDateTime.now().format(TS);
        Path output = storageDir.resolve(safeName + "-" + timestamp + ".json");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateName", templateName == null ? "" : templateName.trim());
        payload.put("savedAt", LocalDateTime.now().toString());
        payload.put("templateBody", templateBody == null ? "" : templateBody);
        payload.put("parametersJson", parametersJson == null ? "" : parametersJson);
        payload.put("htmlOutput", htmlOutput == null ? "" : htmlOutput);

        Files.writeString(
                output,
                gson.toJson(payload),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW
        );

        return output;
    }

    private String sanitizeName(String rawName) {
        String base = (rawName == null || rawName.isBlank()) ? "template" : rawName.trim().toLowerCase();
        String sanitized = base
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "template" : sanitized;
    }
}
