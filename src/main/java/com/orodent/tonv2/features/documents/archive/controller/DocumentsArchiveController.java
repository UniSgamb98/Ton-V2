package com.orodent.tonv2.features.documents.archive.controller;

import com.orodent.tonv2.features.documents.archive.view.DocumentsArchiveView;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.util.List;
import java.util.function.IntConsumer;

public class DocumentsArchiveController {

    private final DocumentsArchiveView view;
    private final TemplateEditorService templateEditorService;
    private final IntConsumer openTemplateEditorAction;

    public DocumentsArchiveController(DocumentsArchiveView view,
                                      TemplateEditorService templateEditorService,
                                      IntConsumer openTemplateEditorAction) {
        this.view = view;
        this.templateEditorService = templateEditorService;
        this.openTemplateEditorAction = openTemplateEditorAction;

        setupActions();
        loadTemplates("");
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadTemplates(newValue));
        view.getTemplatesTable().setOnMouseClicked(event -> {
            DocumentsArchiveView.TemplateRow selectedTemplate = view.getTemplatesTable().getSelectionModel().getSelectedItem();
            if (selectedTemplate == null) {
                return;
            }
            openTemplateEditorAction.accept(selectedTemplate.id());
        });
    }

    private void loadTemplates(String nameFilter) {
        List<DocumentsArchiveView.TemplateRow> rows = templateEditorService.searchSavedTemplates(nameFilter).stream()
                .map(entry -> new DocumentsArchiveView.TemplateRow(entry.id(), entry.name()))
                .toList();
        view.setTemplates(rows);
    }
}
