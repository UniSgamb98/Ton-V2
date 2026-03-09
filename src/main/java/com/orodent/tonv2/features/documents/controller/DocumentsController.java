package com.orodent.tonv2.features.documents.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.documents.view.DocumentsView;

public class DocumentsController {
    private final DocumentsView view;

    public DocumentsController(DocumentsView view, AppController app) {
        this.view = view;

        view.getCreateCompositionButton().setOnAction(e -> app.showCreateComposition());
        view.getCreateDiskModelButton().setOnAction(e -> app.showCreateDiskModel());
        view.getCreateArticleButton().setOnAction(e -> app.showItemSetup());
        view.getProductionButton().setOnAction(e -> app.showBatchProduction());
        view.getPresinterButton().setOnAction(e -> app.showPresintering());
    }

    public DocumentsView getView() {
        return view;
    }
}
