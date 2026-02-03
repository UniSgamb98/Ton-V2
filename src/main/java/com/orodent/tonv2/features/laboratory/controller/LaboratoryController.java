package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.laboratory.view.LaboratoryView;

public class LaboratoryController {
    private final LaboratoryView view;

    public LaboratoryController(LaboratoryView view, AppController app) {
        this.view = view;

        view.getCreateCompositionButton().setOnAction(e -> app.showCreateComposition());
    }

    public LaboratoryView getView() {
        return view;
    }
}
