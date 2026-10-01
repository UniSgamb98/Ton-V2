package com.orodent.tonv2.features.documents.template.controller;

import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.core.ui.form.ConfirmUnsavedChangesDialog;
import com.orodent.tonv2.core.ui.form.DirtyStateTracker;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import javafx.scene.control.TreeItem;
import javafx.util.Duration;

import java.util.concurrent.Executor;

public class TemplateEditorController {

    private final TemplateEditorView view;
    private final TemplateEditorWorkflowService workflowService;
    private final EditorMode editorMode;
    private final DirtyStateTracker dirtyStateTracker;
    private final DebouncedTaskRunner<TemplateEditorWorkflowService.EditorState> initialDataLoader;
    private final DebouncedTaskRunner<TemplateEditorWorkflowService.QueryPayloadState> queryRunner;
    private final DebouncedTaskRunner<TemplateEditorService.SaveResult> saveRunner;
    private String presetJsonPayload = "{}";
    private String queryJsonPayload = "{}";
    private String previewJsonPayload = "{}";
    private boolean applyingInitialState;
    private boolean navigateAfterSuccessfulSave;

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorWorkflowService workflowService,
                                    Executor executor) {
        this(view, workflowService, EditorMode.create(), executor);
    }

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorWorkflowService workflowService,
                                    EditorMode editorMode,
                                    Executor executor) {
        this.view = view;
        this.workflowService = workflowService;
        this.editorMode = editorMode;
        this.initialDataLoader = new DebouncedTaskRunner<>(executor, Duration.ZERO);
        this.queryRunner = new DebouncedTaskRunner<>(executor, Duration.ZERO);
        this.saveRunner = new DebouncedTaskRunner<>(executor, Duration.ZERO);
        this.dirtyStateTracker = new DirtyStateTracker()
                .track("templateName", () -> normalize(view.getTemplateNameField().getText()))
                .track("templateContent", () -> normalize(view.getTemplateContent()))
                .track("sqlQuery", () -> normalize(view.getSqlQuery()))
                .track("presetCode", () -> normalize(view.getPresetSelector().getValue()));

        view.configureEditMode(editorMode.editTemplateId() != null);
        setupActions();
    }

    public void loadInitialData() {
        initialDataLoader.runNow(
                () -> editorMode.editTemplateId() == null
                        ? workflowService.initializeEditorState()
                        : workflowService.initializeEditorStateForEdit(editorMode.editTemplateId()),
                view::showInitialLoading,
                this::applyInitialState,
                error -> view.showLoadError(error.getMessage() == null
                        ? "Errore durante il caricamento del template."
                        : error.getMessage())
        );
    }

    private void applyInitialState(TemplateEditorWorkflowService.EditorState state) {
        applyingInitialState = true;
        view.getTemplateNameField().setText(state.defaultTemplateName());
        view.setTemplateContent(state.defaultTemplateContent());
        view.setSqlQuery(state.sqlQuery() == null ? "" : state.sqlQuery());
        view.getPresetSelector().getItems().setAll(state.presetCodes());
        view.getPresetSelector().setValue(state.defaultPresetCode());
        presetJsonPayload = state.previewJsonPayload();
        queryJsonPayload = "{}";
        syncPreviewPayload();
        applyingInitialState = false;
        dirtyStateTracker.captureInitialState();
        view.showLoadSuccess();
    }

    private void setupActions() {
        view.getSnippetVariableButton().setOnAction(e -> view.insertTemplateSnippet("${variable£}"));
        view.getSnippetIfButton().setOnAction(e -> view.insertTemplateSnippet("""
                <#if condition>
                  <!-- contenuto -->£
                </#if>
                """));
        view.getSnippetListButton().setOnAction(e -> view.insertTemplateSnippet("""
                <#list items as item>
                  <p>Codice: ${item.code}£</p>
                </#list>
                """));
        view.getSnippetAssignButton().setOnAction(e -> view.insertTemplateSnippet("<#assign nomeVariabile = \"valore£\">"));
        view.getSnippetItemsTableButton().setOnAction(e -> view.insertTemplateSnippet("""
                <table>
                  <thead><tr><th>Codice</th><th>Qta</th><th>Altezza</th></tr></thead>
                  <tbody>
                    <#list items as item>
                      <tr><td>${item.code}</td><td>${item.quantity}</td><td>${item.height_mm}£</td></tr>
                    </#list>
                  </tbody>
                </table>
                """));
        view.getSnippetHeaderButton().setOnAction(e -> view.insertTemplateSnippet("<header><h1>${line.name!}£</h1></header>"));
        view.getSnippetFooterButton().setOnAction(e -> view.insertTemplateSnippet("<footer><p>Documento generato automaticamente£</p></footer>"));

        view.getVariablesTree().setOnMouseClicked(event -> insertSelectedVariable());
        view.getFetchDbButton().setOnAction(e -> fetchVariablesFromDb());
        view.getValidateButton().setOnAction(e -> validateTemplate());
        view.getPreviewButton().setOnAction(e -> previewTemplate());
        view.getSaveButton().setOnAction(e -> saveTemplate(false));
        view.getBackButton().setOnAction(e -> navigateBackWithConfirmation());
        view.getPreviewPortraitButton().setOnAction(e -> view.setPreviewPortraitMode());
        view.getPreviewLandscapeButton().setOnAction(e -> view.setPreviewLandscapeMode());
        view.getPresetSelector().valueProperty().addListener((obs, oldVal, newVal) -> applyPreset(newVal));
    }

    private void insertSelectedVariable() {
        TreeItem<String> selected = view.getVariablesTree().getSelectionModel().getSelectedItem();
        if (selected == null || !selected.isLeaf()) return;
        String expression = buildExpression(selected);
        if (!expression.isBlank()) {
            view.insertTemplateSnippet("${" + expression + "}");
            view.focusTemplateEditor();
        }
    }

    private void applyPreset(String presetCode) {
        if (applyingInitialState || presetCode == null || presetCode.isBlank()) return;
        try {
            TemplateEditorWorkflowService.PresetState state = workflowService.applyPreset(presetCode);
            presetJsonPayload = state.previewJsonPayload();
            syncPreviewPayload();
            view.setFeedback("Preset caricato: " + state.presetCode(), false);
        } catch (IllegalArgumentException exception) {
            view.setFeedback(exception.getMessage(), true);
        }
    }

    private void fetchVariablesFromDb() {
        String sqlQuery = view.getSqlQuery();
        queryRunner.runNow(
                () -> workflowService.fetchQueryPayload(sqlQuery),
                view::showQueryLoading,
                state -> {
                    queryJsonPayload = state.queryJsonPayload();
                    syncPreviewPayload();
                    view.showQuerySuccess();
                },
                error -> view.showQueryError(error.getMessage())
        );
    }

    private void syncPreviewPayload() {
        TemplateEditorWorkflowService.CombinedPayloadState combined = workflowService.mergePresetAndQueryPayload(
                presetJsonPayload, queryJsonPayload);
        previewJsonPayload = combined.mergedJsonPayload();
        view.setVariables(combined.variables());
    }

    private void validateTemplate() {
        TemplateEditorService.ValidationResult result = workflowService.validateTemplate(view.getTemplateContent());
        view.setFeedback(result.message(), !result.valid());
    }

    private void previewTemplate() {
        TemplateEditorService.PreviewResult result = workflowService.previewTemplate(
                view.getTemplateContent(), previewJsonPayload);
        if (!result.success()) {
            view.setFeedback(result.htmlOrError(), true);
            if (result.errorLine() != null) view.focusTemplateLine(result.errorLine());
            else view.focusTemplateEditor();
            return;
        }
        view.renderPreview(result.htmlOrError());
        view.setFeedback("Anteprima aggiornata nel pannello di destra.", false);
    }

    private void saveTemplate(boolean navigateOnSuccess) {
        navigateAfterSuccessfulSave = navigateOnSuccess;
        SaveRequest request = new SaveRequest(
                view.getTemplateNameField().getText(),
                view.getTemplateContent(),
                view.getSqlQuery(),
                view.getPresetSelector().getValue());
        saveRunner.runNow(
                () -> editorMode.editTemplateId() == null
                        ? workflowService.saveTemplate(request.name(), request.content(), request.sqlQuery(), request.presetCode())
                        : workflowService.updateTemplate(editorMode.editTemplateId(), request.name(), request.content(),
                                request.sqlQuery(), request.presetCode()),
                view::showSaving,
                this::applySaveResult,
                error -> view.showSaveError(error.getMessage())
        );
    }

    private void applySaveResult(TemplateEditorService.SaveResult result) {
        view.showSaveResult(result.message(), result.success());
        if (!result.success()) return;
        dirtyStateTracker.captureInitialState();
        if (navigateAfterSuccessfulSave && editorMode.navigator() != null) {
            editorMode.navigator().showDocumentsArchive();
        }
    }

    private void navigateBackWithConfirmation() {
        if (editorMode.navigator() == null) return;
        if (!dirtyStateTracker.hasUnsavedChanges()) {
            editorMode.navigator().showDocumentsArchive();
            return;
        }
        ConfirmUnsavedChangesDialog.UserChoice choice = ConfirmUnsavedChangesDialog.show(
                "Modifiche non salvate",
                "Vuoi salvare le modifiche prima di tornare indietro?",
                "Se scegli 'Non salvare' perderai le modifiche effettuate.",
                "Salva modifiche");
        if (choice == ConfirmUnsavedChangesDialog.UserChoice.SAVE) saveTemplate(true);
        else if (choice == ConfirmUnsavedChangesDialog.UserChoice.DISCARD) editorMode.navigator().showDocumentsArchive();
    }

    private String buildExpression(TreeItem<String> leaf) {
        StringBuilder expression = new StringBuilder(sanitizeToken(leaf.getValue()));
        TreeItem<String> current = leaf.getParent();
        while (current != null && current.getParent() != null) {
            String token = sanitizeToken(current.getValue());
            if (!"variabili".equals(token)) {
                expression.insert(0, (token.endsWith("[]") ? token.substring(0, token.length() - 2) : token) + ".");
            }
            current = current.getParent();
        }
        return expression.toString();
    }

    private String sanitizeToken(String raw) {
        if (raw == null) return "";
        int separator = raw.indexOf(" = ");
        return separator >= 0 ? raw.substring(0, separator).trim() : raw.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    public void dispose() {
        initialDataLoader.cancel();
        queryRunner.cancel();
        saveRunner.cancel();
    }

    private record SaveRequest(String name, String content, String sqlQuery, String presetCode) {}

    public record EditorMode(Integer editTemplateId, DocumentsNavigator navigator) {
        public static EditorMode create() {
            return new EditorMode(null, null);
        }

        public static EditorMode edit(int templateId, DocumentsNavigator navigator) {
            return new EditorMode(templateId, navigator);
        }
    }
}
