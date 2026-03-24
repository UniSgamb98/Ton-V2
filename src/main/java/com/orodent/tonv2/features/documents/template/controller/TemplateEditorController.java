package com.orodent.tonv2.features.documents.template.controller;

import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import javafx.scene.control.TreeItem;

import java.sql.Connection;
import java.util.List;
import java.util.function.Supplier;

public class TemplateEditorController {

    private final TemplateEditorView view;
    private final TemplateEditorService service;
    private final Supplier<Connection> connectionSupplier;

    public TemplateEditorController(TemplateEditorView view,
                                    TemplateEditorService service,
                                    Supplier<Connection> connectionSupplier) {
        this.view = view;
        this.service = service;
        this.connectionSupplier = connectionSupplier;

        setupDefaults();
        setupActions();
    }

    private void setupDefaults() {
        view.getTemplateNameField().setText("NuovoTemplate");
        view.getTemplateEditor().setValue("""
                <html>
                  <body>
                    <h1>${line.name!\"Documento\"}</h1>
                  </body>
                </html>
                """);
        view.getSampleJsonArea().setText("""
                {
                  "line": {"name": "Linea A"},
                  "composition": {"id": 10, "version": 3, "num_layers": 5},
                  "items": [{"code": "A01", "quantity": 3, "height_mm": 14.2}]
                }
                """);
        view.setVariables(defaultVariables());
    }

    private List<TemplateEditorService.VariableNode> defaultVariables() {
        return List.of(
                new TemplateEditorService.VariableNode("line", List.of(
                        new TemplateEditorService.VariableNode("name", List.of())
                )),
                new TemplateEditorService.VariableNode("composition", List.of(
                        new TemplateEditorService.VariableNode("id", List.of()),
                        new TemplateEditorService.VariableNode("version", List.of()),
                        new TemplateEditorService.VariableNode("num_layers", List.of())
                )),
                new TemplateEditorService.VariableNode("items[]", List.of(
                        new TemplateEditorService.VariableNode("code", List.of()),
                        new TemplateEditorService.VariableNode("quantity", List.of()),
                        new TemplateEditorService.VariableNode("height_mm", List.of())
                ))
        );
    }

    private void setupActions() {
        view.getSnippetVariableButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("${variable}\n"));
        view.getSnippetIfButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <#if condition>
                  <!-- contenuto -->
                </#if>
                """));
        view.getSnippetListButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <#list items as item>
                  <p>Codice: ${item.code}</p>
                </#list>
                """));
        view.getSnippetAssignButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<#assign nomeVariabile = \"valore\">\n"));
        view.getSnippetItemsTableButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("""
                <table>
                  <thead><tr><th>Codice</th><th>Qta</th><th>Altezza</th></tr></thead>
                  <tbody>
                    <#list items as item>
                      <tr><td>${item.code}</td><td>${item.quantity}</td><td>${item.height_mm}</td></tr>
                    </#list>
                  </tbody>
                </table>
                """));
        view.getSnippetHeaderButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<header><h1>${line.name!}</h1></header>\n"));
        view.getSnippetFooterButton().setOnAction(e -> view.getTemplateEditor().insertSnippet("<footer><p>Documento generato automaticamente</p></footer>\n"));

        view.getVariablesTree().setOnMouseClicked(event -> {
            TreeItem<String> selected = view.getVariablesTree().getSelectionModel().getSelectedItem();
            if (selected == null || !selected.isLeaf()) {
                return;
            }

            String expression = buildExpression(selected);
            if (!expression.isBlank()) {
                view.getTemplateEditor().insertSnippet("${" + expression + "}");
            }
        });

        view.getFetchDbButton().setOnAction(e -> fetchVariablesFromDb());
        view.getValidateButton().setOnAction(e -> validateTemplate());
        view.getPreviewButton().setOnAction(e -> previewTemplate());
        view.getSaveButton().setOnAction(e -> saveTemplate());
    }

    private String buildExpression(TreeItem<String> leaf) {
        StringBuilder expression = new StringBuilder(leaf.getValue());
        TreeItem<String> current = leaf.getParent();

        while (current != null && current.getParent() != null) {
            String token = current.getValue();
            if (!"variabili".equals(token)) {
                String normalized = token.endsWith("[]") ? token.substring(0, token.length() - 2) : token;
                expression.insert(0, normalized + ".");
            }
            current = current.getParent();
        }

        return expression.toString();
    }

    private void fetchVariablesFromDb() {
        try (Connection connection = connectionSupplier.get()) {
            List<TemplateEditorService.VariableNode> variables = service.extractVariablesFromQuery(
                    view.getSqlEditor().getValue(),
                    connection
            );
            view.setVariables(variables);
            view.setFeedback("Variabili aggiornate dalla query SQL.", false);
        } catch (Exception ex) {
            view.setFeedback(ex.getMessage(), true);
        }
    }

    private void validateTemplate() {
        TemplateEditorService.ValidationResult result = service.validateTemplate(view.getTemplateEditor().getValue());
        view.setFeedback(result.message(), !result.valid());
    }

    private void previewTemplate() {
        TemplateEditorService.PreviewResult result = service.previewTemplate(
                view.getTemplateEditor().getValue(),
                view.getSampleJsonArea().getText()
        );
        view.getOutputArea().setText(result.htmlOrError());
        view.setFeedback(result.success() ? "Anteprima generata." : "Errore durante anteprima.", !result.success());
    }

    private void saveTemplate() {
        TemplateEditorService.SaveResult result = service.saveTemplate(
                view.getTemplateNameField().getText(),
                view.getTemplateEditor().getValue(),
                view.getSqlEditor().getValue(),
                view.getPresetSelector().getValue()
        );
        view.setFeedback(result.message(), !result.success());
    }
}
