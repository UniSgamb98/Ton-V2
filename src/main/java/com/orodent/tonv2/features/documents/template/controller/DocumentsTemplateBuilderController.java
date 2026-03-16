package com.orodent.tonv2.features.documents.template.controller;

import com.google.gson.JsonSyntaxException;
import com.orodent.tonv2.features.documents.template.service.DocumentTemplateService;
import com.orodent.tonv2.features.documents.template.service.TemplateRenderResult;
import com.orodent.tonv2.features.documents.template.service.TemplateStorageService;
import com.orodent.tonv2.features.documents.template.view.DocumentsTemplateBuilderView;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DocumentsTemplateBuilderController {
    private final DocumentsTemplateBuilderView view;
    private final DocumentTemplateService service;
    private final TemplateStorageService storageService;

    public DocumentsTemplateBuilderController(
            DocumentsTemplateBuilderView view,
            DocumentTemplateService service,
            TemplateStorageService storageService
    ) {
        this.view = view;
        this.service = service;
        this.storageService = storageService;

        initializeDefaults();
        view.getRenderButton().setOnAction(e -> onRender());
        view.getOpenPreviewButton().setOnAction(e -> onOpenPreview());
        view.getSaveTemplateButton().setOnAction(e -> onSaveTemplate());
    }

    private void initializeDefaults() {
        view.getTemplateArea().setText(service.defaultTemplate());
        view.getParametersArea().setText(service.defaultParametersJson());
        onRender();
    }

    private void onRender() {
        try {
            Map<String, Object> parameters = service.parseParameters(view.getParametersArea().getText());
            TemplateRenderResult result = service.render(view.getTemplateArea().getText(), parameters);

            view.getResolvedMarkupArea().setText(result.resolvedMarkup());
            view.getHtmlPreviewArea().setText(result.html());
            view.getWarningsArea().setText(String.join("\n", result.warnings()));
        } catch (JsonSyntaxException ex) {
            view.getWarningsArea().setText("JSON non valido: " + ex.getMessage());
        }
    }

    private void onOpenPreview() {
        onRender();

        String html = view.getHtmlPreviewArea().getText();
        if (html == null || html.isBlank()) {
            appendWarning("Impossibile aprire anteprima: HTML vuoto.");
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            appendWarning("Anteprima non disponibile: apertura browser non supportata in questo ambiente.");
            return;
        }

        try {
            Path tempFile = Files.createTempFile("ton-document-preview-", ".html");
            Files.writeString(tempFile, html, StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(tempFile.toUri());
            appendWarning("Anteprima aperta: " + tempFile);
        } catch (IOException ex) {
            appendWarning("Errore apertura anteprima: " + ex.getMessage());
        }
    }

    private void onSaveTemplate() {
        onRender();

        try {
            Path saved = storageService.saveTemplate(
                    view.getTemplateNameField().getText(),
                    view.getTemplateArea().getText(),
                    view.getParametersArea().getText()
            );
            appendWarning("Template salvato in: " + saved.toAbsolutePath());
        } catch (IOException ex) {
            appendWarning("Errore salvataggio template: " + ex.getMessage());
        }
    }

    private void appendWarning(String message) {
        String current = view.getWarningsArea().getText();
        if (current == null || current.isBlank()) {
            view.getWarningsArea().setText(message);
            return;
        }

        view.getWarningsArea().setText(current + "\n" + message);
    }

    public DocumentsTemplateBuilderView getView() {
        return view;
    }
}
