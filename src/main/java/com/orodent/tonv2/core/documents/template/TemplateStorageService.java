package com.orodent.tonv2.core.documents.template;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateStorageService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path storageDir;
    private final Gson gson;

    public TemplateStorageService(Path storageDir) {
        this.storageDir = storageDir;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Path saveTemplate(String templateName, String templateBody, String parametersJson) throws IOException {
        Files.createDirectories(storageDir);

        String safeName = sanitizeName(templateName);
        String timestamp = LocalDateTime.now().format(TS);
        Path output = storageDir.resolve(safeName + "-" + timestamp + ".json");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateName", templateName == null ? "" : templateName.trim());
        payload.put("savedAt", LocalDateTime.now().toString());
        payload.put("templateBody", templateBody == null ? "" : templateBody);
        payload.put("parametersJson", parametersJson == null ? "" : parametersJson);

        Files.writeString(
                output,
                gson.toJson(payload),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW
        );

        return output;
    }

    public List<SavedTemplateRef> listTemplates() {
        try {
            if (!Files.exists(storageDir)) {
                return List.of();
            }

            List<SavedTemplateRef> refs = new ArrayList<>();
            Files.list(storageDir)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString).reversed())
                    .forEach(path -> {
                        String displayName = path.getFileName().toString();
                        try {
                            JsonObject obj = gson.fromJson(Files.readString(path), JsonObject.class);
                            if (obj != null && obj.has("templateName")) {
                                String n = obj.get("templateName").getAsString();
                                if (n != null && !n.isBlank()) {
                                    displayName = n;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        refs.add(new SavedTemplateRef(path, displayName));
                    });

            return refs;
        } catch (IOException e) {
            return List.of();
        }
    }

    public StoredTemplate loadTemplate(Path file) throws IOException {
        JsonObject obj = gson.fromJson(Files.readString(file), JsonObject.class);
        if (obj == null) {
            throw new IOException("Template non valido: " + file);
        }

        String templateName = obj.has("templateName") ? obj.get("templateName").getAsString() : file.getFileName().toString();
        String templateBody = obj.has("templateBody") ? obj.get("templateBody").getAsString() : "";
        String parametersJson = obj.has("parametersJson") ? obj.get("parametersJson").getAsString() : "{}";
        return new StoredTemplate(file, templateName, templateBody, parametersJson);
    }

    private String sanitizeName(String rawName) {
        String base = (rawName == null || rawName.isBlank()) ? "template" : rawName.trim().toLowerCase();
        String sanitized = base
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "template" : sanitized;
    }

    public record SavedTemplateRef(Path path, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    public record StoredTemplate(Path path, String templateName, String templateBody, String parametersJson) {
    }
}
