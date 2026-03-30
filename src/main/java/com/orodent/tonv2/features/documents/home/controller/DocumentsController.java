package com.orodent.tonv2.features.documents.home.controller;

import com.orodent.tonv2.app.DocumentsNavigator;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;

public class DocumentsController {

    private final DocumentsView view;

    public DocumentsController(DocumentsView view, DocumentsNavigator navigator) {
        this.view = view;

        view.getCreateDocumentButton().setOnAction(e -> navigator.showCreate());
        view.getArchiveButton().setOnAction(e -> navigator.showArchive());
        view.getSearchButton().setOnAction(e -> navigator.showArchive());
    }

    public DocumentsView getView() {
        return view;
    }
}
