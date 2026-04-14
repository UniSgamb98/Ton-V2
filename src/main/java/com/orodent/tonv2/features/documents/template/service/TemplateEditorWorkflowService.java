package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringDocumentParamsService;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TemplateEditorWorkflowService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final String DEFAULT_TEMPLATE_NAME = "NuovoTemplate";
    private static final String DEFAULT_PRESET_CODE = TemplatePresetCodes.PRODUCTION;
    private static final String PRESINTERING_PRESET_CODE = TemplatePresetCodes.FIRING;
    private static final String DEFAULT_TEMPLATE_CONTENT = """
            <html>
              <body>
                <h1>${line.name!"Documento"}</h1>
              </body>
            </html>
            """;

    private final TemplateEditorService templateEditorService;
    private final Supplier<Connection> connectionSupplier;
    private final BatchProductionDocumentParamsService batchPresetService;
    private final PresinteringDocumentParamsService presinteringPresetService;
    private final Map<String, Map<String, Object>> presetPayloadByCode = new LinkedHashMap<>();
    private final Gson gson = new Gson();

    public TemplateEditorWorkflowService(TemplateEditorService templateEditorService,
                                         Supplier<Connection> connectionSupplier,
                                         BatchProductionDocumentParamsService batchPresetService,
                                         PresinteringDocumentParamsService presinteringPresetService) {
        this.templateEditorService = templateEditorService;
        this.connectionSupplier = connectionSupplier;
        this.batchPresetService = batchPresetService;
        this.presinteringPresetService = presinteringPresetService;

        loadPresets();
    }

    public EditorState initializeEditorState() {
        Map<String, Object> defaultPayload = presetPayloadByCode.getOrDefault(DEFAULT_PRESET_CODE, Map.of());
        return new EditorState(
                DEFAULT_TEMPLATE_NAME,
                DEFAULT_TEMPLATE_CONTENT,
                List.copyOf(presetPayloadByCode.keySet()),
                DEFAULT_PRESET_CODE,
                templateEditorService.toJson(defaultPayload),
                templateEditorService.extractVariablesFromParamsMap(defaultPayload),
                null,
                false
        );
    }

    public EditorState initializeEditorStateForEdit(int templateId) {
        TemplateEditorService.TemplateSnapshot template = templateEditorService.getTemplateById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template non trovato: id=" + templateId);
        }

        String selectedPresetCode = resolvePresetCode(template.presetCode());
        Map<String, Object> selectedPreset = presetPayloadByCode.getOrDefault(selectedPresetCode, Map.of());

        return new EditorState(
                template.name(),
                template.templateContent(),
                List.copyOf(presetPayloadByCode.keySet()),
                selectedPresetCode,
                templateEditorService.toJson(selectedPreset),
                templateEditorService.extractVariablesFromParamsMap(selectedPreset),
                template.id(),
                true
        );
    }

    public PresetState applyPreset(String presetCode) {
        if (presetCode == null || presetCode.isBlank()) {
            throw new IllegalArgumentException("Preset non valido.");
        }

        Map<String, Object> payload = presetPayloadByCode.get(presetCode);
        if (payload == null) {
            throw new IllegalArgumentException("Preset non trovato: " + presetCode);
        }

        return new PresetState(
                presetCode,
                templateEditorService.toJson(payload),
                templateEditorService.extractVariablesFromParamsMap(payload)
        );
    }

    public QueryPayloadState fetchQueryPayload(String sqlQuery) {
        try (Connection connection = connectionSupplier.get()) {
            TemplateEditorService.QueryVariablesResult result = templateEditorService.extractVariablesFromQuery(sqlQuery, connection);
            return new QueryPayloadState(result.sampleJsonPayload());
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    public CombinedPayloadState mergePresetAndQueryPayload(String presetJsonPayload, String queryJsonPayload) {
        Map<String, Object> presetMap = parseJsonToMap(presetJsonPayload);
        Map<String, Object> queryMap = parseJsonToMap(queryJsonPayload);

        Map<String, Object> merged = deepMerge(presetMap, queryMap);

        return new CombinedPayloadState(
                templateEditorService.toJson(merged),
                templateEditorService.extractVariablesFromParamsMap(merged)
        );
    }

    public TemplateEditorService.ValidationResult validateTemplate(String templateText) {
        return templateEditorService.validateTemplate(templateText);
    }

    public TemplateEditorService.PreviewResult previewTemplate(String templateText, String jsonPayload) {
        return templateEditorService.previewTemplate(templateText, jsonPayload);
    }

    public TemplateEditorService.SaveResult saveTemplate(String templateName,
                                                         String templateText,
                                                         String sqlQuery,
                                                         String presetCode) {
        return templateEditorService.saveTemplate(templateName, templateText, sqlQuery, presetCode);
    }

    public TemplateEditorService.SaveResult updateTemplate(int templateId,
                                                           String templateName,
                                                           String templateText,
                                                           String sqlQuery,
                                                           String presetCode) {
        return templateEditorService.updateTemplate(templateId, templateName, templateText, sqlQuery, presetCode);
    }

    private String resolvePresetCode(String requestedPresetCode) {
        if (requestedPresetCode != null && presetPayloadByCode.containsKey(requestedPresetCode)) {
            return requestedPresetCode;
        }
        return DEFAULT_PRESET_CODE;
    }

    private void loadPresets() {
        Map<String, Object> batchPreset = batchPresetService.buildParams(
                BatchProductionDocumentParamsService.ParamsRequest.preset("Preset automatico da DB", 1)
        );
        presetPayloadByCode.put(DEFAULT_PRESET_CODE, batchPreset);
        presetPayloadByCode.put(
                PRESINTERING_PRESET_CODE,
                presinteringPresetService.buildPresetParams("Preset automatico presinterizzazione")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> extra) {
        Map<String, Object> merged = new LinkedHashMap<>(base);

        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            String key = entry.getKey();
            Object extraValue = entry.getValue();
            Object baseValue = merged.get(key);

            if (baseValue instanceof Map<?, ?> baseMap && extraValue instanceof Map<?, ?> extraMap) {
                merged.put(
                        key,
                        deepMerge(
                                (Map<String, Object>) baseMap,
                                (Map<String, Object>) extraMap
                        )
                );
            } else {
                merged.put(key, extraValue);
            }
        }

        return merged;
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> parsed = gson.fromJson(json, MAP_TYPE);
        return parsed == null ? new LinkedHashMap<>() : parsed;
    }

    public record EditorState(String defaultTemplateName,
                              String defaultTemplateContent,
                              List<String> presetCodes,
                              String defaultPresetCode,
                              String previewJsonPayload,
                              List<TemplateEditorService.VariableNode> variables,
                              Integer templateId,
                              boolean editMode) {
    }

    public record PresetState(String presetCode,
                              String previewJsonPayload,
                              List<TemplateEditorService.VariableNode> variables) {
    }

    public record QueryPayloadState(String queryJsonPayload) {
    }

    public record CombinedPayloadState(String mergedJsonPayload,
                                       List<TemplateEditorService.VariableNode> variables) {
    }
}
