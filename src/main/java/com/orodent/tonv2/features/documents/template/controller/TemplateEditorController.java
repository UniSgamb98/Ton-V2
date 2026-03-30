package com.orodent.tonv2.features.documents.template.controller;

import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;

import java.util.Objects;

public class TemplateEditorController {

    private final TemplateEditorView view;
    private final TemplateEditorWorkflowService workflowService;
    private final EditorMode editorMode;
    private String presetJsonPayload;
    private String queryJsonPayload;
    private String previewJsonPayload;

    private String initialTemplateName = "";
    private String initialTemplateContent = "";
    private String initialSqlQuery = "";
    private String initialPresetCode = "";

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorWorkflowService workflowService) {
        this(view, workflowService, EditorMode.create());
    }

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorWorkflowService workflowService,
                                    EditorMode editorMode) {
        this.view = view;
        this.workflowService = workflowService;
        this.editorMode = editorMode;

        setupDefaults();
        setupActions();
    }

    private void setupDefaults() {
        TemplateEditorWorkflowService.EditorState editorState = editorMode.editTemplateId() == null
                ? workflowService.initializeEditorState()
                : workflowService.initializeEditorStateForEdit(editorMode.editTemplateId());

        view.getTemplateNameField().setText(editorState.defaultTemplateName());
        view.getTemplateEditor().setValue(editorState.defaultTemplateContent());

        view.getPresetSelector().setDisable(false);
        view.getPresetSelector().getItems().setAll(editorState.presetCodes());
        view.getPresetSelector().setValue(editorState.defaultPresetCode());

        view.getSqlEditor().setValue(editorMode.initialSqlQuery() == null ? "" : editorMode.initialSqlQuery());
        view.configureEditMode(editorMode.editTemplateId() != null);

        presetJsonPayload = editorState.previewJsonPayload();
        queryJsonPayload = "{}";
        syncPreviewPayload();
        captureInitialState();
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
        view.getBackButton().setOnAction(e -> navigateBackWithConfirmation());
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
            presetJsonPayload = presetState.previewJsonPayload();
            syncPreviewPayload();
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
            TemplateEditorWorkflowService.QueryPayloadState queryState = workflowService.fetchQueryPayload(
                    view.getSqlEditor().getValue()
            );
            queryJsonPayload = queryState.queryJsonPayload();
            syncPreviewPayload();
            view.setFeedback("Variabili query aggiunte alle variabili preset.", false);
        } catch (Exception ex) {
            view.setFeedback(ex.getMessage(), true);
        }
    }

    private void syncPreviewPayload() {
        TemplateEditorWorkflowService.CombinedPayloadState combined = workflowService.mergePresetAndQueryPayload(
                presetJsonPayload,
                queryJsonPayload
        );
        previewJsonPayload = combined.mergedJsonPayload();
        view.setVariables(combined.variables());
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
        TemplateEditorService.SaveResult result;
        if (editorMode.editTemplateId() == null) {
            result = workflowService.saveTemplate(
                    view.getTemplateNameField().getText(),
                    view.getTemplateEditor().getValue(),
                    view.getSqlEditor().getValue(),
                    view.getPresetSelector().getValue()
            );
        } else {
            result = workflowService.updateTemplate(
                    editorMode.editTemplateId(),
                    view.getTemplateNameField().getText(),
                    view.getTemplateEditor().getValue(),
                    view.getSqlEditor().getValue(),
                    view.getPresetSelector().getValue()
            );
        }

        view.setFeedback(result.message(), !result.success());
        if (result.success()) {
            captureInitialState();
        }
    }

    private void navigateBackWithConfirmation() {
        if (editorMode.backAction() == null) {
            return;
        }

        if (!hasUnsavedChanges()) {
            editorMode.backAction().run();
            return;
        }

        ButtonType saveAndBack = new ButtonType("Salva modifiche", ButtonBar.ButtonData.YES);
        ButtonType discardAndBack = new ButtonType("Non salvare", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Modifiche non salvate");
        alert.setHeaderText("Vuoi salvare le modifiche prima di tornare indietro?");
        alert.setContentText("Se scegli 'Non salvare' perderai le modifiche effettuate.");
        alert.getButtonTypes().setAll(saveAndBack, discardAndBack, cancel);

        ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == saveAndBack) {
            saveTemplate();
            if (!hasUnsavedChanges()) {
                editorMode.backAction().run();
            }
            return;
        }

        if (result == discardAndBack) {
            editorMode.backAction().run();
        }
    }

    private boolean hasUnsavedChanges() {
        return !Objects.equals(initialTemplateName, normalize(view.getTemplateNameField().getText()))
                || !Objects.equals(initialTemplateContent, normalize(view.getTemplateEditor().getValue()))
                || !Objects.equals(initialSqlQuery, normalize(view.getSqlEditor().getValue()))
                || !Objects.equals(initialPresetCode, normalize(view.getPresetSelector().getValue()));
    }

    private void captureInitialState() {
        initialTemplateName = normalize(view.getTemplateNameField().getText());
        initialTemplateContent = normalize(view.getTemplateEditor().getValue());
        initialSqlQuery = normalize(view.getSqlEditor().getValue());
        initialPresetCode = normalize(view.getPresetSelector().getValue());
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    public record EditorMode(Integer editTemplateId, String initialSqlQuery, Runnable backAction) {
        public static EditorMode create() {
            return new EditorMode(null, "", null);
        }

        public static EditorMode edit(int templateId, String initialSqlQuery, Runnable backAction) {
            return new EditorMode(templateId, initialSqlQuery, backAction);
        }
    }
}
