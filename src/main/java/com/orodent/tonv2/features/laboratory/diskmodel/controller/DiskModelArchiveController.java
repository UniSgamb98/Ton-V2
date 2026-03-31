package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.DiskModelArchiveView;

import java.util.List;

public class DiskModelArchiveController {

    private final DiskModelArchiveView view;
    private final LaboratoryNavigator navigator;
    private final DiskModelArchiveService service;

    public DiskModelArchiveController(DiskModelArchiveView view,
                                      LaboratoryNavigator navigator,
                                      DiskModelArchiveService service) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;

        setupActions();
        loadDiskModels("");
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadDiskModels(newValue));
        view.getDiskModelsTable().setOnMouseClicked(event -> {
            DiskModelArchiveView.DiskModelRow selected = view.getDiskModelsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateDiskModel(selected.id());
        });
    }

    private void loadDiskModels(String nameFilter) {
        List<DiskModelArchiveView.DiskModelRow> rows = service.searchDiskModels(nameFilter).stream()
                .map(model -> new DiskModelArchiveView.DiskModelRow(model.id(), model.code() + " (v" + model.version() + ")"))
                .toList();

        view.setDiskModels(rows);
    }
}
