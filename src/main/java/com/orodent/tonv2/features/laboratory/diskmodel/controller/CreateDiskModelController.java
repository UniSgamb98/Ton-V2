package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final AppController app;
    private final CreateDiskModelService service;

    public CreateDiskModelController(CreateDiskModelView view,
                                     AppController app,
                                     CreateDiskModelService service) {
        this.view = view;
        this.app = app;
        this.service = service;

        setupActions();
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> save());
    }

    private void save() {
        try {
            CreateDiskModelService.CreateDiskModelData modelData = new CreateDiskModelService.CreateDiskModelData(
                    trimToNull(view.getCode()),
                    parseDouble(view.getDiameter(), "Diametro"),
                    parseDouble(view.getSuperiorOvermaterial(), "Overmaterial superiore default"),
                    parseDouble(view.getInferiorOvermaterial(), "Overmaterial inferiore default"),
                    parseDouble(view.getPressure(), "Pressione"),
                    parseDouble(view.getGramsPerMm(), "Grammi per mm"),
                    parseInteger(view.getNumLayers(), "Numero strati")
            );

            List<CreateDiskModelService.LayerData> layers = parseLayers();
            List<CreateDiskModelService.HeightRangeData> ranges = parseRanges();

            service.createDiskModel(modelData, layers, ranges);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Modello disco salvato");
            ok.setContentText("Il nuovo modello è stato registrato correttamente.");
            ok.showAndWait();

            app.showLaboratory();
        } catch (IllegalArgumentException ex) {
            showError("Validazione dati", ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Errore salvataggio modello", "Non è stato possibile salvare il modello disco: " + ex.getMessage());
        }
    }

    private List<CreateDiskModelService.LayerData> parseLayers() {
        List<CreateDiskModelService.LayerData> layers = new ArrayList<>();

        for (CreateDiskModelView.LayerPercentageDraft draft : view.getLayerPercentageDrafts()) {
            Double percentage = parseDouble(draft.percentage(), "Percentuale layer " + draft.layerNumber());
            layers.add(new CreateDiskModelService.LayerData(draft.layerNumber(), percentage));
        }

        return layers;
    }

    private List<CreateDiskModelService.HeightRangeData> parseRanges() {
        List<CreateDiskModelService.HeightRangeData> ranges = new ArrayList<>();

        for (CreateDiskModelView.HeightRangeDraft draft : view.getRangeDrafts()) {
            ranges.add(new CreateDiskModelService.HeightRangeData(
                    parseDouble(draft.minHeight(), "Min altezza fascia"),
                    parseDouble(draft.maxHeight(), "Max altezza fascia"),
                    parseDouble(draft.superiorOvermaterial(), "Overmaterial superiore fascia"),
                    parseDouble(draft.inferiorOvermaterial(), "Overmaterial inferiore fascia")
            ));
        }

        return ranges;
    }

    private Integer parseInteger(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un intero valido.");
        }
    }

    private Double parseDouble(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un numero valido.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showError(String header, String content) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setHeaderText(header);
        error.setContentText(content);
        error.showAndWait();
    }
}
