package com.orodent.tonv2.features.documents.archive.controller;

import com.orodent.tonv2.app.DocumentsNavigator;
import com.orodent.tonv2.features.documents.archive.view.DocumentsArchiveView;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.util.List;

public class DocumentsArchiveController {

    private final DocumentsArchiveView view;
    private final TemplateEditorService templateEditorService;
    private final DocumentsNavigator documentsNavigator;

    public DocumentsArchiveController(DocumentsArchiveView view,
                                      TemplateEditorService templateEditorService,
                                      DocumentsNavigator documentsNavigator) {
        this.view = view;
        this.templateEditorService = templateEditorService;
        this.documentsNavigator = documentsNavigator;

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
            documentsNavigator.showEditTemplate(selectedTemplate.id());
        });
    }

    private void loadTemplates(String nameFilter) {
        List<DocumentsArchiveView.TemplateRow> rows = templateEditorService.searchSavedTemplates(nameFilter).stream()
                .map(entry -> new DocumentsArchiveView.TemplateRow(entry.id(), entry.name()))
                .toList();
        view.setTemplates(rows);
    }
}
