package com.orodent.tonv2.features.documents.home.controller;

import com.orodent.tonv2.features.documents.home.view.DocumentsView;

public class DocumentsController {

    private final DocumentsView view;

    public DocumentsController(DocumentsView view) {
        this.view = view;
    }

    public DocumentsView getView() {
        return view;
    }
}
