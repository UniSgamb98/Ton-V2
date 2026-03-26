package com.orodent.tonv2.features.documents.template.controller;

import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import javafx.scene.control.TreeItem;

public class TemplateEditorController {

    private final TemplateEditorView view;
    private final TemplateEditorWorkflowService workflowService;
    private String previewJsonPayload;

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorWorkflowService workflowService) {
        this.view = view;
        this.workflowService = workflowService;

        setupDefaults();
        setupActions();
    }

    private void setupDefaults() {
        TemplateEditorWorkflowService.EditorState editorState = workflowService.initializeEditorState();

        view.getTemplateNameField().setText(editorState.defaultTemplateName());
        view.getTemplateEditor().setValue(editorState.defaultTemplateContent());

        view.getPresetSelector().setDisable(false);
        view.getPresetSelector().getItems().setAll(editorState.presetCodes());
        view.getPresetSelector().setValue(editorState.defaultPresetCode());

        previewJsonPayload = editorState.previewJsonPayload();
        view.setVariables(editorState.variables());
    }

    private void setupActions() {
        view.getSnippetVariableButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("${variable£}"));
        view.getSnippetIfButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <#if condition>
                  <!-- contenuto -->£
                </#if>
                """));
        view.getSnippetListButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <#list items as item>
                  <p>Codice: ${item.code}£</p>
                </#list>
                """));
        view.getSnippetAssignButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<#assign nomeVariabile = \"valore£\">"));
        view.getSnippetItemsTableButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <table>
                  <thead><tr><th>Codice</th><th>Qta</th><th>Altezza</th></tr></thead>
                  <tbody>
                    <#list items as item>
                      <tr><td>${item.code}</td><td>${item.quantity}</td><td>${item.height_mm}£</td></tr>
                    </#list>
                  </tbody>
                </table>
                """));
        view.getSnippetHeaderButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<header><h1>${line.name!}£</h1></header>"));
        view.getSnippetFooterButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<footer><p>Documento generato automaticamente£</p></footer>"));

        view.getVariablesTree().setOnMouseClicked(event -> {
            TreeItem<String> selected = view.getVariablesTree().getSelectionModel().getSelectedItem();
            if (selected == null || !selected.isLeaf()) {
                return;
            }

            String expression = buildExpression(selected);
            if (!expression.isBlank()) {
                view.getTemplateEditor().insertSnippet("${" + expression + "}");
                view.getTemplateEditor().focusEditor();
            }
        });

        view.getFetchDbButton().setOnAction(e -> fetchVariablesFromDb());
        view.getValidateButton().setOnAction(e -> validateTemplate());
        view.getPreviewButton().setOnAction(e -> previewTemplate());
        view.getSaveButton().setOnAction(e -> saveTemplate());
        view.getPreviewPortraitButton().setOnAction(e -> view.setPreviewPortraitMode());
        view.getPreviewLandscapeButton().setOnAction(e -> view.setPreviewLandscapeMode());
        view.getPresetSelector().valueProperty().addListener((obs, oldVal, newVal) -> applyPreset(newVal));
    }

    private void applyPreset(String presetCode) {
        if (presetCode == null || presetCode.isBlank()) {
            return;
        }

        try {
            TemplateEditorWorkflowService.PresetState presetState = workflowService.applyPreset(presetCode);
            previewJsonPayload = presetState.previewJsonPayload();
            view.setVariables(presetState.variables());
            view.setFeedback("Preset caricato: " + presetState.presetCode(), false);
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        }
    }

    private String buildExpression(TreeItem<String> leaf) {
        StringBuilder expression = new StringBuilder(sanitizeToken(leaf.getValue()));
        TreeItem<String> current = leaf.getParent();

        while (current != null && current.getParent() != null) {
            String token = sanitizeToken(current.getValue());
            if (!"variabili".equals(token)) {
                String normalized = token.endsWith("[]") ? token.substring(0, token.length() - 2) : token;
                expression.insert(0, normalized + ".");
            }
            current = current.getParent();
        }

        return expression.toString();
    }

    private String sanitizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        int sep = raw.indexOf(" = ");
        return sep >= 0 ? raw.substring(0, sep).trim() : raw.trim();
    }

    private void fetchVariablesFromDb() {
        try {
            TemplateEditorService.QueryVariablesResult result = workflowService.fetchVariablesFromDb(
                    view.getSqlEditor().getValue()
            );
            view.setVariables(result.variables());
            previewJsonPayload = result.sampleJsonPayload();
            view.setFeedback("Variabili aggiornate dalla query SQL.", false);
        } catch (Exception ex) {
            view.setFeedback(ex.getMessage(), true);
        }
    }

    private void validateTemplate() {
        TemplateEditorService.ValidationResult result = workflowService.validateTemplate(view.getTemplateEditor().getValue());
        view.setFeedback(result.message(), !result.valid());
    }

    private void previewTemplate() {
        TemplateEditorService.PreviewResult result = workflowService.previewTemplate(
                view.getTemplateEditor().getValue(),
                previewJsonPayload
        );

        if (!result.success()) {
            view.setFeedback(result.htmlOrError(), true);
            if (result.errorLine() != null) {
                view.getTemplateEditor().focusLine(result.errorLine());
            } else {
                view.getTemplateEditor().focusEditor();
            }
            return;
        }

        view.renderPreview(result.htmlOrError());
        view.setFeedback("Anteprima aggiornata nel pannello di destra.", false);
    }

    private void saveTemplate() {
        TemplateEditorService.SaveResult result = workflowService.saveTemplate(
                view.getTemplateNameField().getText(),
                view.getTemplateEditor().getValue(),
                view.getSqlEditor().getValue(),
                view.getPresetSelector().getValue()
        );
        view.setFeedback(result.message(), !result.success());
    }
}
