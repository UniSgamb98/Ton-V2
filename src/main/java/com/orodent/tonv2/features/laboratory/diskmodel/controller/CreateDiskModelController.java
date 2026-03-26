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
                    requireText(view.getCode(), "Codice modello"),
                    parsePositive(view.getDiameter(), "Diametro"),
                    parseNonNegative(view.getSuperiorOvermaterial(), "Overmaterial superiore default"),
                    parseNonNegative(view.getInferiorOvermaterial(), "Overmaterial inferiore default"),
                    parsePositive(view.getPressure(), "Pressione"),
                    parsePositive(view.getGramsPerMm(), "Grammi per mm"),
                    parsePositiveInt(view.getNumLayers(), "Numero strati")
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
            double percentage = parsePositive(draft.percentage(), "Percentuale layer " + draft.layerNumber());
            layers.add(new CreateDiskModelService.LayerData(draft.layerNumber(), percentage));
        }

        return layers;
    }

    private List<CreateDiskModelService.HeightRangeData> parseRanges() {
        List<CreateDiskModelService.HeightRangeData> ranges = new ArrayList<>();

        for (CreateDiskModelView.HeightRangeDraft draft : view.getRangeDrafts()) {
            if (isEmptyRange(draft)) {
                continue;
            }

            double min = parseNonNegative(draft.minHeight(), "Min altezza fascia");
            double max = parsePositive(draft.maxHeight(), "Max altezza fascia");
            double superior = parseNonNegative(draft.superiorOvermaterial(), "Overmaterial superiore fascia");
            double inferior = parseNonNegative(draft.inferiorOvermaterial(), "Overmaterial inferiore fascia");

            ranges.add(new CreateDiskModelService.HeightRangeData(min, max, superior, inferior));
        }

        return ranges;
    }

    private boolean isEmptyRange(CreateDiskModelView.HeightRangeDraft draft) {
        return isBlank(draft.minHeight())
                && isBlank(draft.maxHeight())
                && isBlank(draft.superiorOvermaterial())
                && isBlank(draft.inferiorOvermaterial());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " è obbligatorio.");
        }
        return value.trim();
    }

    private double parsePositive(String raw, String fieldName) {
        double value = parseDouble(raw, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " deve essere maggiore di 0.");
        }
        return value;
    }

    private double parseNonNegative(String raw, String fieldName) {
        double value = parseDouble(raw, fieldName);
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " non può essere negativo.");
        }
        return value;
    }


    private int parsePositiveInt(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " è obbligatorio.");
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " deve essere maggiore di 0.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un intero valido.");
        }
    }

    private double parseDouble(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " è obbligatorio.");
        }

        try {
            String normalized = raw.trim().replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un numero valido.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String header, String content) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setHeaderText(header);
        error.setContentText(content);
        error.showAndWait();
    }
}
