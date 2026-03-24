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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
    private final Button snippetItemsTableButton = new Button("Tabella items");
    private final Button snippetHeaderButton = new Button("Header documento");
    private final Button snippetFooterButton = new Button("Footer");

    private final TreeView<String> variablesTree = new TreeView<>();

    private final Button fetchDbButton = new Button("Fetch dati DB");
    private final Button validateButton = new Button("Valida");
    private final Button previewButton = new Button("Anteprima");
    private final Button saveButton = new Button("Salva");

    private final TextArea sampleJsonArea = new TextArea();
    private final TextArea outputArea = new TextArea();
    private final Label feedbackLabel = new Label();

    public TemplateEditorView() {
        setSpacing(12);
        setPadding(new Insets(16));

        presetSelector.setPromptText("Preset parametri (placeholder)");
        presetSelector.setDisable(true);

        templateNameField.setPromptText("Nome template");

        HBox topBar = new HBox(10,
                new Label("Preset:"), presetSelector,
                new Label("Nome:"), templateNameField
        );
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox snippetsBox = new VBox(8,
                new Label("Blocchi FreeMarker"),
                snippetVariableButton,
                snippetIfButton,
                snippetListButton,
                snippetAssignButton,
                snippetItemsTableButton,
                snippetHeaderButton,
                snippetFooterButton
        );
        snippetsBox.setPrefWidth(180);

        TreeItem<String> root = new TreeItem<>("variabili");
        root.setExpanded(true);
        variablesTree.setRoot(root);
        variablesTree.setShowRoot(false);

        VBox variablesBox = new VBox(6, new Label("Variabili disponibili"), variablesTree);
        variablesBox.setPrefWidth(240);

        VBox templateEditorBox = new VBox(6, new Label("Template FreeMarker"), templateEditor);
        VBox.setVgrow(templateEditor, Priority.ALWAYS);

        SplitPane upperPane = new SplitPane(snippetsBox, templateEditorBox, variablesBox);
        upperPane.setDividerPositions(0.16, 0.73);

        VBox sqlBox = new VBox(6, new Label("Query SQL (solo SELECT)"), sqlEditor);
        VBox.setVgrow(sqlEditor, Priority.ALWAYS);

        sampleJsonArea.setPromptText("Dati JSON di esempio per anteprima...");
        sampleJsonArea.setPrefRowCount(5);

        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(8);

        HBox actions = new HBox(10, fetchDbButton, validateButton, previewButton, saveButton);

        getChildren().addAll(
                header,
                topBar,
                upperPane,
                sqlBox,
                new Label("JSON anteprima"),
                sampleJsonArea,
                actions,
                feedbackLabel,
                new Label("Output / Anteprima HTML"),
                outputArea
        );

        VBox.setVgrow(upperPane, Priority.ALWAYS);
        VBox.setVgrow(sqlBox, Priority.ALWAYS);
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
        TreeItem<String> item = new TreeItem<>(node.name());
        item.setExpanded(true);
        for (TemplateEditorService.VariableNode child : node.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
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
    public TextArea getSampleJsonArea() { return sampleJsonArea; }
    public TextArea getOutputArea() { return outputArea; }
}
