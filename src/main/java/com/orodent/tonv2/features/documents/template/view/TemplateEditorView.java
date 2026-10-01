package com.orodent.tonv2.features.documents.template.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.core.ui.editor.CodeEditorLanguage;
import com.orodent.tonv2.core.ui.editor.CodeMirrorEditor;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.util.List;

public class TemplateEditorView extends VBox {

    private final AppHeader header = new AppHeader("Nuovo documento - Template Builder");
    private final ComboBox<String> presetSelector = new ComboBox<>();
    private final TextField templateNameField = new TextField();

    private final CodeMirrorEditor templateEditor = new CodeMirrorEditor(CodeEditorLanguage.HTML, "");
    private final CodeMirrorEditor sqlEditor = new CodeMirrorEditor(CodeEditorLanguage.SQL, "SELECT line.name AS line.name FROM line");

    private final Button snippetVariableButton = new Button("Variabile");
    private final Button snippetIfButton = new Button("If");
    private final Button snippetListButton = new Button("Ciclo For");
    private final Button snippetAssignButton = new Button("Assign");
    private final Button snippetItemsTableButton = new Button("Tabella");
    private final Button snippetHeaderButton = new Button("Header");
    private final Button snippetFooterButton = new Button("Footer");

    private final TreeView<String> variablesTree = new TreeView<>();

    private final Button fetchDbButton = new Button("Fetch dati DB");
    private final Button validateButton = new Button("Valida");
    private final Button previewButton = new Button("Anteprima");
    private final Button saveButton = new Button("Salva");
    private final Button backButton = new Button("Indietro");
    private final Button previewPortraitButton = new Button("A4 Verticale");
    private final Button previewLandscapeButton = new Button("A4 Orizzontale");

    private final TextArea feedbackArea = new TextArea();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label operationStatusLabel = new Label();

    private final WebView previewWebView = new WebView();
    private final StackPane previewPageFrame = new StackPane(previewWebView);

    public TemplateEditorView() {
        setSpacing(12);
        setPadding(new Insets(16));

        presetSelector.setPromptText("Preset parametri (placeholder)");
        presetSelector.setDisable(true);
        templateNameField.setPromptText("Nome template");

        HBox topBar = new HBox(10,
                topLabel("Preset:"), presetSelector,
                topLabel("Nome:"), templateNameField
        );
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox snippetsRow = new HBox(8,
                snippetVariableButton,
                snippetIfButton,
                snippetListButton,
                snippetAssignButton,
                snippetItemsTableButton,
                snippetHeaderButton,
                snippetFooterButton
        );
        snippetsRow.setAlignment(Pos.CENTER_LEFT);

        VBox templateBox = new VBox(6, label("Template FreeMarker"), templateEditor);
        VBox.setVgrow(templateEditor, Priority.ALWAYS);

        TreeItem<String> root = new TreeItem<>("variabili");
        root.setExpanded(true);
        variablesTree.setRoot(root);
        variablesTree.setShowRoot(false);
        VBox variablesBox = new VBox(6, label("Variabili disponibili"), variablesTree);
        variablesBox.setPrefWidth(220);
        variablesBox.setMinWidth(160);
        VBox.setVgrow(variablesTree, Priority.ALWAYS);

        VBox queryBox = new VBox(6, label("Query per Variabili"), sqlEditor);
        VBox.setVgrow(sqlEditor, Priority.ALWAYS);

        SplitPane queryAndVariables = new SplitPane(queryBox, variablesBox);
        queryAndVariables.setOrientation(Orientation.HORIZONTAL);
        queryAndVariables.setDividerPositions(0.68);
        queryAndVariables.setMinHeight(220);
        HBox.setHgrow(queryAndVariables, Priority.ALWAYS);

        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        operationStatusLabel.setVisible(false);
        operationStatusLabel.setManaged(false);

        HBox actions = new HBox(10, fetchDbButton, validateButton, previewButton, saveButton, backButton,
                progressIndicator, operationStatusLabel);
        backButton.setVisible(false);
        backButton.setManaged(false);

        SplitPane editorSplitPane = new SplitPane(templateBox, queryAndVariables);
        editorSplitPane.setOrientation(Orientation.VERTICAL);
        editorSplitPane.setDividerPositions(0.60);
        VBox.setVgrow(editorSplitPane, Priority.ALWAYS);

        feedbackArea.setEditable(false);
        feedbackArea.setWrapText(true);
        feedbackArea.setPrefRowCount(3);
        feedbackArea.setMinHeight(90);
        feedbackArea.setStyle("-fx-control-inner-background: #f8fafc; -fx-text-fill: #166534;");

        VBox leftPane = new VBox(10, snippetsRow, editorSplitPane, actions, feedbackArea);
        VBox.setVgrow(editorSplitPane, Priority.ALWAYS);

        previewPageFrame.setStyle("-fx-background-color: white; -fx-border-color: #94a3b8; -fx-border-width: 1;");
        previewPageFrame.setMaxSize(794, 1123);
        previewPageFrame.setMinSize(450, 620);
        previewWebView.getEngine().loadContent("<html><body style='font-family: Arial, sans-serif;'><h3>Anteprima HTML</h3><p>Premi 'Anteprima' per vedere il render.</p></body></html>");

        HBox previewHeader = new HBox(8, label("Anteprima HTML"), previewPortraitButton, previewLandscapeButton);
        previewHeader.setAlignment(Pos.CENTER_LEFT);

        VBox previewPane = new VBox(6, previewHeader, previewPageFrame);
        previewPane.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(previewPageFrame, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftPane, previewPane);
        splitPane.setDividerPositions(0.62);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(
                header,
                topBar,
                splitPane
        );

        setPreviewPortraitMode();
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 700;");
        return label;
    }

    private Label topLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: 700;");
        return label;
    }

    public void setVariables(List<TemplateEditorService.VariableNode> variables) {
        TreeItem<String> root = new TreeItem<>("variabili");
        root.setExpanded(true);
        for (TemplateEditorService.VariableNode node : variables) {
            root.getChildren().add(toTreeItem(node));
        }
        variablesTree.setRoot(root);
        variablesTree.setShowRoot(false);
    }

    private TreeItem<String> toTreeItem(TemplateEditorService.VariableNode node) {
        TreeItem<String> item = new TreeItem<>(formatVariableLabel(node));
        item.setExpanded(true);
        for (TemplateEditorService.VariableNode child : node.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
    }


    private String formatVariableLabel(TemplateEditorService.VariableNode node) {
        if (node.sampleValue() == null || node.sampleValue().isBlank()) {
            return node.name();
        }
        return node.name() + " = " + node.sampleValue();
    }

    public void renderPreview(String html) {
        previewWebView.getEngine().loadContent(html == null ? "" : html, "text/html");
    }

    public void setPreviewPortraitMode() {
        previewPageFrame.setMaxSize(794, 1123);
        previewPageFrame.setPrefSize(794, 1123);
        previewPortraitButton.setDisable(true);
        previewLandscapeButton.setDisable(false);
    }

    public void setPreviewLandscapeMode() {
        previewPageFrame.setMaxSize(1123, 794);
        previewPageFrame.setPrefSize(1123, 794);
        previewPortraitButton.setDisable(false);
        previewLandscapeButton.setDisable(true);
    }


    public void configureEditMode(boolean editMode) {
        if (editMode) {
            saveButton.setText("Salva modifiche");
            backButton.setVisible(true);
            backButton.setManaged(true);
        } else {
            saveButton.setText("Salva");
            backButton.setVisible(false);
            backButton.setManaged(false);
        }
    }

    public void setFeedback(String message, boolean error) {
        feedbackArea.setText(message == null ? "" : message);
        feedbackArea.setStyle(error
                ? "-fx-control-inner-background: #fff1f2; -fx-text-fill: #b91c1c;"
                : "-fx-control-inner-background: #f0fdf4; -fx-text-fill: #166534;");
    }

    public void showInitialLoading() {
        setBusy(true, "Caricamento editor...");
        setActionsDisabled(true);
        presetSelector.setDisable(true);
    }

    public void showLoadSuccess() {
        setBusy(false, "");
        setActionsDisabled(false);
        presetSelector.setDisable(false);
    }

    public void showLoadError(String message) {
        setBusy(false, "Caricamento non riuscito");
        setActionsDisabled(true);
        backButton.setDisable(false);
        setFeedback(message, true);
    }

    public void showQueryLoading() {
        setBusy(true, "Esecuzione query...");
        fetchDbButton.setDisable(true);
    }

    public void showQuerySuccess() {
        setBusy(false, "");
        fetchDbButton.setDisable(false);
        setFeedback("Variabili query aggiunte alle variabili preset.", false);
    }

    public void showQueryError(String message) {
        setBusy(false, "Query non riuscita");
        fetchDbButton.setDisable(false);
        setFeedback(message == null ? "Errore durante l'esecuzione della query." : message, true);
    }

    public void showSaving() {
        setBusy(true, "Salvataggio template...");
        saveButton.setDisable(true);
        backButton.setDisable(true);
    }

    public void showSaveResult(String message, boolean success) {
        setBusy(false, success ? "Salvataggio completato" : "Salvataggio non riuscito");
        saveButton.setDisable(false);
        backButton.setDisable(false);
        setFeedback(message, !success);
    }

    public void showSaveError(String message) {
        showSaveResult(message == null ? "Errore durante il salvataggio del template." : message, false);
    }

    private void setBusy(boolean busy, String message) {
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
        operationStatusLabel.setText(message == null ? "" : message);
        operationStatusLabel.setVisible(message != null && !message.isBlank());
        operationStatusLabel.setManaged(operationStatusLabel.isVisible());
    }

    private void setActionsDisabled(boolean disabled) {
        fetchDbButton.setDisable(disabled);
        validateButton.setDisable(disabled);
        previewButton.setDisable(disabled);
        saveButton.setDisable(disabled);
        backButton.setDisable(disabled);
    }

    public AppHeader getHeader() { return header; }
    public ComboBox<String> getPresetSelector() { return presetSelector; }
    public TextField getTemplateNameField() { return templateNameField; }
    public String getTemplateContent() { return templateEditor.getValue(); }
    public void setTemplateContent(String content) { templateEditor.setValue(content); }
    public void insertTemplateSnippet(String snippet) { templateEditor.insertSnippet(snippet); }
    public void focusTemplateEditor() { templateEditor.focusEditor(); }
    public void focusTemplateLine(int lineNumber) { templateEditor.focusLine(lineNumber); }
    public String getSqlQuery() { return sqlEditor.getValue(); }
    public void setSqlQuery(String query) { sqlEditor.setValue(query); }
    public Button getSnippetVariableButton() { return snippetVariableButton; }
    public Button getSnippetIfButton() { return snippetIfButton; }
    public Button getSnippetListButton() { return snippetListButton; }
    public Button getSnippetAssignButton() { return snippetAssignButton; }
    public Button getSnippetItemsTableButton() { return snippetItemsTableButton; }
    public Button getSnippetHeaderButton() { return snippetHeaderButton; }
    public Button getSnippetFooterButton() { return snippetFooterButton; }
    public TreeView<String> getVariablesTree() { return variablesTree; }
    public Button getFetchDbButton() { return fetchDbButton; }
    public Button getValidateButton() { return validateButton; }
    public Button getPreviewButton() { return previewButton; }
    public Button getSaveButton() { return saveButton; }
    public Button getBackButton() { return backButton; }
    public Button getPreviewPortraitButton() { return previewPortraitButton; }
    public Button getPreviewLandscapeButton() { return previewLandscapeButton; }
}
