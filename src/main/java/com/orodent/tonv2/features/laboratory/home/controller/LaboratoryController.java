package com.orodent.tonv2.features.laboratory.home.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.laboratory.home.view.LaboratoryView;

public class LaboratoryController {
    private final LaboratoryView view;

    public LaboratoryController(LaboratoryView view, AppController app) {
        this.view = view;

        view.getCreateCompositionButton().setOnAction(e -> app.showCreateComposition());
        view.getCreateDiskModelButton().setOnAction(e -> app.showCreateDiskModel());
        view.getCreateArticleButton().setOnAction(e -> app.showItemSetup());
        view.getProduceButton().setOnAction(e -> app.showBatchProduction());
        view.getPresinterButton().setOnAction(e -> app.showPresintering());
    }

    public LaboratoryView getView() {
        return view;
    }
}
