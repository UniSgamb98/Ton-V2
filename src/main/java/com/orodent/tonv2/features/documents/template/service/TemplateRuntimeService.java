package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class TemplateRuntimeService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final String RUNTIME_TEMPLATE_KEY = "runtime-template";

    private final Gson gson;

    public TemplateRuntimeService(Gson gson) {
        this.gson = gson;
    }

    public ValidationResult validateTemplate(String templateText) {
        try {
            compileTemplate(templateText);
            return ValidationResult.ok();
        } catch (IOException e) {
            ErrorDetails details = extractErrorDetails(e, "Errore validazione");
            return ValidationResult.error(details.message(), details.line(), details.column());
        }
    }

    public PreviewResult previewTemplate(String templateText, String sampleJsonData) {
        try {
            Template template = compileTemplate(templateText);
            Map<String, Object> dataModel = parseJsonData(sampleJsonData);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return PreviewResult.ok(writer.toString());
        } catch (IOException | TemplateException e) {
            ErrorDetails details = extractErrorDetails(e, "Errore anteprima");
            return PreviewResult.error(details.message(), details.line(), details.column());
        }
    }

    Template compileTemplate(String templateText) throws IOException {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate(RUNTIME_TEMPLATE_KEY, templateText == null ? "" : templateText);

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setTemplateLoader(loader);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);

        return cfg.getTemplate(RUNTIME_TEMPLATE_KEY);
    }

    private Map<String, Object> parseJsonData(String sampleJsonData) {
        String payload = sampleJsonData == null ? "" : sampleJsonData.trim();
        if (payload.isBlank()) {
            return new HashMap<>();
        }
        Map<String, Object> parsed = gson.fromJson(payload, MAP_TYPE);
        return parsed == null ? new HashMap<>() : parsed;
    }

    private ErrorDetails extractErrorDetails(Exception exception, String prefix) {
        if (exception instanceof ParseException parseException) {
            String description = safeMessage(parseException.getEditorMessage(), parseException.getMessage());
            return new ErrorDetails(prefix + ": " + description, parseException.getLineNumber(), parseException.getColumnNumber());
        }

        if (exception instanceof TemplateException templateException) {
            String description = safeMessage(templateException.getBlamedExpressionString(), templateException.getMessage());
            return new ErrorDetails(prefix + ": " + description, templateException.getLineNumber(), templateException.getColumnNumber());
        }

        return new ErrorDetails(prefix + ": " + safeMessage(exception.getMessage(), exception.toString()), null, null);
    }

    private String safeMessage(String preferred, String fallback) {
        String normalizedPreferred = preferred == null ? "" : preferred.trim();
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        return fallback == null ? "Errore sconosciuto." : fallback.trim();
    }

    private static String appendLineColumn(String message, Integer line, Integer column) {
        if (line == null || line <= 0) {
            return message;
        }
        if (column != null && column > 0) {
            return message + System.lineSeparator() + "Riga: " + line + ", Colonna: " + column;
        }
        return message + System.lineSeparator() + "Riga: " + line;
    }

    public record ValidationResult(boolean valid, String message) {
        static ValidationResult ok() { return new ValidationResult(true, "Template valido."); }
        static ValidationResult error(String message, Integer line, Integer column) {
            return new ValidationResult(false, appendLineColumn(message, line, column));
        }
    }

    public record PreviewResult(boolean success, String htmlOrError, Integer errorLine) {
        static PreviewResult ok(String html) { return new PreviewResult(true, html, null); }
        static PreviewResult error(String error, Integer line, Integer column) {
            return new PreviewResult(false, appendLineColumn(error, line, column), line);
        }
    }

    private record ErrorDetails(String message, Integer line, Integer column) {}
}
