package com.orodent.tonv2.features.laboratory.home.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.home.view.LaboratoryView;

public class LaboratoryController {
    private final LaboratoryView view;

    public LaboratoryController(LaboratoryView view, LaboratoryNavigator navigator) {
        this.view = view;

        view.getCreateCompositionButton().setOnAction(e -> navigator.showCreateComposition());
        view.getCreateDiskModelButton().setOnAction(e -> navigator.showCreateDiskModel());
        view.getArchiveDiskModelsButton().setOnAction(e -> navigator.showLaboratoryDiskModelArchive());
        view.getArchiveCompositionsButton().setOnAction(e -> navigator.showLaboratoryCompositionArchive());
        view.getCreateArticleButton().setOnAction(e -> navigator.showItemSetup());
        view.getProduceButton().setOnAction(e -> navigator.showBatchProduction());
        view.getPresinterButton().setOnAction(e -> navigator.showPresintering());
    }

    public LaboratoryView getView() {
        return view;
    }
}
