package com.orodent.tonv2.features.documents.home.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;

public class DocumentsController {

    private final DocumentsView view;

    public DocumentsController(DocumentsView view, AppController app) {
        this.view = view;

        view.getCreateDocumentButton().setOnAction(e -> app.showDocumentsCreate());
        view.getArchiveButton().setOnAction(e -> app.showDocumentsArchive());
        view.getSearchButton().setOnAction(e -> app.showDocumentsSearch());
    }

    public DocumentsView getView() {
        return view;
    }
}
