package com.orodent.tonv2.features.documents.home.controller;

import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;

public class DocumentsController {

    private final DocumentsView view;

    public DocumentsController(DocumentsView view, DocumentsNavigator navigator) {
        this.view = view;

        view.getCreateDocumentButton().setOnAction(e -> navigator.showDocumentsCreate());
        view.getArchiveButton().setOnAction(e -> navigator.showDocumentsArchive());
        view.getSearchButton().setOnAction(e -> navigator.showDocuments());
    }

    public DocumentsView getView() {
        return view;
    }
}
