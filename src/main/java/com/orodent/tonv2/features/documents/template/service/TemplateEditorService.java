package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orodent.tonv2.core.database.implementation.DocumentTemplateRepositoryImpl;
import com.orodent.tonv2.core.database.repository.DocumentTemplateRepository;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TemplateEditorService {

    private final TemplateRuntimeService runtime;
    private final TemplateVariableService variables;
    private final TemplateStorageService storage;
    private String lastBatchTemplateName;

    public TemplateEditorService(Supplier<Connection> connectionSupplier) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.runtime = new TemplateRuntimeService(gson);
        this.variables = new TemplateVariableService(gson);
        DocumentTemplateRepository templateRepository = new DocumentTemplateRepositoryImpl(connectionSupplier.get());
        this.storage = new TemplateStorageService(templateRepository);
    }

    public ValidationResult validateTemplate(String templateText) {
        TemplateRuntimeService.ValidationResult result = runtime.validateTemplate(templateText);
        return new ValidationResult(result.valid(), result.message());
    }

    public PreviewResult previewTemplate(String templateText, String sampleJsonData) {
        TemplateRuntimeService.PreviewResult result = runtime.previewTemplate(templateText, sampleJsonData);
        return new PreviewResult(result.success(), result.htmlOrError(), result.errorLine());
    }

    public SaveResult saveTemplate(String templateName, String templateText, String sqlQuery, String presetCode) {
        String normalizedName = templateName == null ? "" : templateName.trim();
        if (normalizedName.isBlank()) {
            return SaveResult.error("Nome template obbligatorio.");
        }

        ValidationResult validation = validateTemplate(templateText);
        if (!validation.valid()) {
            return SaveResult.error("Impossibile salvare: " + validation.message());
        }

        storage.saveTemplate(normalizedName, templateText, sqlQuery, presetCode);
        return SaveResult.ok("Template salvato su database.");
    }

    public QueryVariablesResult extractVariablesFromQuery(String sqlQuery, Connection connection) {
        TemplateVariableService.QueryVariablesResult result = variables.extractVariablesFromQuery(sqlQuery, connection);
        return new QueryVariablesResult(toVariableNodes(result.variables()), result.sampleJsonPayload());
    }

    public List<VariableNode> extractVariablesFromParamsMap(Map<String, Object> paramsMap) {
        return toVariableNodes(variables.mapToTree(paramsMap));
    }

    public String toJson(Map<String, Object> payload) {
        return variables.toJson(payload);
    }

    public List<TemplateSnapshot> getSavedTemplates() {
        return storage.loadTemplates().stream()
                .map(snapshot -> new TemplateSnapshot(
                        snapshot.name(),
                        snapshot.templateContent(),
                        snapshot.sqlQuery(),
                        snapshot.presetCode(),
                        snapshot.savedAt()
                ))
                .toList();
    }

    public String getTemplateContentByName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return null;
        }

        for (TemplateSnapshot snapshot : getSavedTemplates()) {
            if (snapshot.name().equals(templateName)) {
                return snapshot.templateContent();
            }
        }
        return null;
    }

    public String getLastBatchTemplateName() {
        return lastBatchTemplateName;
    }

    public void setLastBatchTemplateName(String templateName) {
        lastBatchTemplateName = templateName == null || templateName.isBlank() ? null : templateName.trim();
    }

    private List<VariableNode> toVariableNodes(List<TemplateVariableService.VariableNode> variableNodes) {
        return variableNodes.stream()
                .map(this::toVariableNode)
                .toList();
    }

    private VariableNode toVariableNode(TemplateVariableService.VariableNode node) {
        return new VariableNode(node.name(), node.sampleValue(), toVariableNodes(node.children()));
    }

    public record ValidationResult(boolean valid, String message) {
    }

    public record PreviewResult(boolean success, String htmlOrError, Integer errorLine) {
    }

    public record SaveResult(boolean success, String message) {
        static SaveResult ok(String message) { return new SaveResult(true, message); }
        static SaveResult error(String message) { return new SaveResult(false, message); }
    }

    public record VariableNode(String name, String sampleValue, List<VariableNode> children) {}

    public record QueryVariablesResult(List<VariableNode> variables, String sampleJsonPayload) {}

    public record TemplateSnapshot(String name, String templateContent, String sqlQuery, String presetCode, Instant savedAt) {}
}
