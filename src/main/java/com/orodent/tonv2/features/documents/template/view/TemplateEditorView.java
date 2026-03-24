package com.orodent.tonv2.features.documents.template.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.view.components.CodeMirrorEditor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.util.List;

public class TemplateEditorView extends VBox {

    private final AppHeader header = new AppHeader("Nuovo documento - Template Builder");
    private final ComboBox<String> presetSelector = new ComboBox<>();
    private final TextField templateNameField = new TextField();

    private final CodeMirrorEditor templateEditor = new CodeMirrorEditor("htmlmixed", "");
    private final CodeMirrorEditor sqlEditor = new CodeMirrorEditor("text/x-sql", "SELECT line.name AS line.name FROM line");

    private final Button snippetVariableButton = new Button("Variabile");
    private final Button snippetIfButton = new Button("If");
    private final Button snippetListButton = new Button("Ciclo items");
    private final Button snippetAssignButton = new Button("Assign");
    private final Button snippetItemsTableButton = new Button("Tabella");
    private final Button snippetHeaderButton = new Button("Header");
    private final Button snippetFooterButton = new Button("Footer");

    private final TreeView<String> variablesTree = new TreeView<>();

    private final Button fetchDbButton = new Button("Fetch dati DB");
    private final Button validateButton = new Button("Valida");
    private final Button previewButton = new Button("Anteprima");
    private final Button saveButton = new Button("Salva");

    private final Label feedbackLabel = new Label();

    private final WebView previewWebView = new WebView();

    public TemplateEditorView() {
        setSpacing(12);
        setPadding(new Insets(16));

        presetSelector.setPromptText("Preset parametri (placeholder)");
        presetSelector.setDisable(true);
        templateNameField.setPromptText("Nome template");

        HBox topBar = new HBox(10,
                label("Preset:"), presetSelector,
                label("Nome:"), templateNameField
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
        variablesBox.setPrefWidth(400);
        variablesBox.setMinWidth(400);
        VBox.setVgrow(variablesTree, Priority.ALWAYS);

        VBox queryBox = new VBox(6, label("Query per Variabili"), sqlEditor);
        VBox.setVgrow(sqlEditor, Priority.ALWAYS);

        HBox queryAndVariables = new HBox(10, queryBox, variablesBox);
        HBox.setHgrow(queryBox, Priority.ALWAYS);
        HBox.setHgrow(variablesBox, Priority.SOMETIMES);

        HBox actions = new HBox(10, fetchDbButton, validateButton, previewButton, saveButton);

        VBox leftPane = new VBox(10,
                snippetsRow,
                templateBox,
                queryAndVariables,
                actions,
                feedbackLabel
        );
        VBox.setVgrow(templateBox, Priority.ALWAYS);
        VBox.setVgrow(queryAndVariables, Priority.ALWAYS);

        previewWebView.getEngine().loadContent("<html><body><h3>Anteprima HTML</h3><p>Premi 'Anteprima' per vedere il render.</p></body></html>");
        VBox previewPane = new VBox(6, label("Anteprima HTML"), previewWebView);
        VBox.setVgrow(previewWebView, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftPane, previewPane);
        splitPane.setDividerPositions(0.62);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(
                header,
                topBar,
                splitPane
        );
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 700;");
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

    public void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    public AppHeader getHeader() { return header; }
    public ComboBox<String> getPresetSelector() { return presetSelector; }
    public TextField getTemplateNameField() { return templateNameField; }
    public CodeMirrorEditor getTemplateEditor() { return templateEditor; }
    public CodeMirrorEditor getSqlEditor() { return sqlEditor; }
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
}
