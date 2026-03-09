package com.orodent.tonv2.features.documents.template.controller;

import com.google.gson.JsonSyntaxException;
import com.orodent.tonv2.features.documents.template.service.DocumentTemplateService;
import com.orodent.tonv2.features.documents.template.service.TemplateRenderResult;
import com.orodent.tonv2.features.documents.template.view.DocumentsTemplateBuilderView;

import java.util.Map;

public class DocumentsTemplateBuilderController {
    private final DocumentsTemplateBuilderView view;
    private final DocumentTemplateService service;

    public DocumentsTemplateBuilderController(DocumentsTemplateBuilderView view, DocumentTemplateService service) {
        this.view = view;
        this.service = service;

        initializeDefaults();
        view.getRenderButton().setOnAction(e -> onRender());
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

    public DocumentsTemplateBuilderView getView() {
        return view;
    }
}
