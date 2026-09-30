package com.orodent.tonv2.features.documents.archive.controller;

import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.documents.archive.view.DocumentsArchiveView;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class DocumentsArchiveController {

    private final DocumentsArchiveView view;
    private final TemplateEditorService templateEditorService;
    private final DocumentsNavigator navigator;
    private final DebouncedTaskRunner<List<DocumentsArchiveView.TemplateRow>> loader;

    public DocumentsArchiveController(DocumentsArchiveView view,
                                      TemplateEditorService templateEditorService,
                                      DocumentsNavigator navigator,
                                      Executor backgroundExecutor) {
        this.view = view;
        this.templateEditorService = templateEditorService;
        this.navigator = navigator;
        this.loader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(300));

        setupActions();
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadDebounced(newValue));
        view.getTemplatesTable().setOnMouseClicked(event -> {
            DocumentsArchiveView.TemplateRow selectedTemplate = view.getTemplatesTable().getSelectionModel().getSelectedItem();
            if (selectedTemplate == null) {
                return;
            }
            navigator.showDocumentsEditTemplate(selectedTemplate.id());
        });
    }

    public void loadInitialData() {
        loader.runNow(
                () -> searchTemplates(""),
                view::showLoading,
                view::showTemplates,
                error -> view.showLoadError()
        );
    }

    private void loadDebounced(String nameFilter) {
        loader.runDebounced(
                () -> searchTemplates(nameFilter),
                view::showLoading,
                view::showTemplates,
                error -> view.showLoadError()
        );
    }

    private List<DocumentsArchiveView.TemplateRow> searchTemplates(String nameFilter) {
        return templateEditorService.searchSavedTemplates(nameFilter).stream()
                .map(entry -> new DocumentsArchiveView.TemplateRow(entry.id(), entry.name()))
                .toList();
    }
}
