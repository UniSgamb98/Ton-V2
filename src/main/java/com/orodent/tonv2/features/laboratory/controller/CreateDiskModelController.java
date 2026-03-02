package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.laboratory.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.view.CreateDiskModelView;
import javafx.scene.control.Alert;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final AppController app;
    private final CreateDiskModelService createDiskModelService;

    public CreateDiskModelController(CreateDiskModelView view,
                                     AppController app,
                                     CreateDiskModelService createDiskModelService) {
        this.view = view;
        this.app = app;
        this.createDiskModelService = createDiskModelService;

        setupActions();
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> save());
    }

    private void save() {
        CreateDiskModelService.SaveRequest request = new CreateDiskModelService.SaveRequest(
                view.getCode(),
                view.getDiameter(),
                view.getSuperiorOvermaterial(),
                view.getInferiorOvermaterial(),
                view.getPressure(),
                view.getGramsPerMm(),
                view.getRangeDrafts(),
                view.getLayerDrafts()
        );

        CreateDiskModelService.SaveResult result = createDiskModelService.save(request);

        Alert alert = new Alert(result.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(result.title());
        alert.setContentText(result.message());
        alert.showAndWait();

        if (result.success()) {
            app.showLaboratory();
        }
    }
}
