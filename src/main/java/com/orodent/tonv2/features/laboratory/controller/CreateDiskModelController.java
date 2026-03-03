package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.features.laboratory.view.CreateDiskModelView;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final AppController app;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelHeightOvermaterialRepository overmaterialRepo;

    public CreateDiskModelController(CreateDiskModelView view,
                                     AppController app,
                                     BlankModelRepository blankModelRepo,
                                     BlankModelHeightOvermaterialRepository overmaterialRepo) {
        this.view = view;
        this.app = app;
        this.blankModelRepo = blankModelRepo;
        this.overmaterialRepo = overmaterialRepo;

        setupActions();
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> save());
    }

    private void save() {
        String code = requireText(view.getCode(), "Codice modello");
        if (code == null) return;

        Double diameter = parsePositive(view.getDiameter(), "Diametro");
        Double superior = parseNonNegative(view.getSuperiorOvermaterial(), "Overmaterial superiore default");
        Double inferior = parseNonNegative(view.getInferiorOvermaterial(), "Overmaterial inferiore default");
        Double pressure = parsePositive(view.getPressure(), "Pressione");
        Double gramsPerMm = parsePositive(view.getGramsPerMm(), "Grammi per mm");
        Integer numLayers = parsePositiveInt(view.getNumLayers(), "Numero strati");

        if (diameter == null || superior == null || inferior == null || pressure == null || gramsPerMm == null || numLayers == null) {
            return;
        }

        List<BlankModelHeightOvermaterial> ranges = new ArrayList<>();
        for (CreateDiskModelView.HeightRangeDraft draft : view.getRangeDrafts()) {
            if (isEmptyRange(draft)) {
                continue;
            }

            Double min = parseNonNegative(draft.minHeight(), "Min altezza fascia");
            Double max = parsePositive(draft.maxHeight(), "Max altezza fascia");
            Double sup = parseNonNegative(draft.superiorOvermaterial(), "Overmaterial superiore fascia");
            Double inf = parseNonNegative(draft.inferiorOvermaterial(), "Overmaterial inferiore fascia");
            if (min == null || max == null || sup == null || inf == null) {
                return;
            }
            if (max <= min) {
                showError("Intervallo altezza non valido", "Ogni fascia deve avere max altezza maggiore della min altezza.");
                return;
            }

            ranges.add(new BlankModelHeightOvermaterial(0, 0, min, max, sup, inf));
        }

        try {
            BlankModel model = blankModelRepo.insert(code, diameter, superior, inferior, pressure, gramsPerMm, numLayers);

            for (BlankModelHeightOvermaterial range : ranges) {
                overmaterialRepo.insert(new BlankModelHeightOvermaterial(
                        0,
                        model.id(),
                        range.minHeightMm(),
                        range.maxHeightMm(),
                        range.superiorOvermaterialMm(),
                        range.inferiorOvermaterialMm()
                ));
            }

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Modello disco salvato");
            ok.setContentText("Il nuovo modello è stato registrato correttamente.");
            ok.showAndWait();

            app.showLaboratory();
        } catch (RuntimeException ex) {
            showError("Errore salvataggio modello", "Non è stato possibile salvare il modello disco: " + ex.getMessage());
        }
    }

    private boolean isEmptyRange(CreateDiskModelView.HeightRangeDraft draft) {
        return isBlank(draft.minHeight())
                && isBlank(draft.maxHeight())
                && isBlank(draft.superiorOvermaterial())
                && isBlank(draft.inferiorOvermaterial());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            showError("Campo obbligatorio mancante", fieldName + " è obbligatorio.");
            return null;
        }
        return value.trim();
    }

    private Double parsePositive(String raw, String fieldName) {
        Double value = parseDouble(raw, fieldName);
        if (value == null) return null;
        if (value <= 0) {
            showError("Valore non valido", fieldName + " deve essere maggiore di 0.");
            return null;
        }
        return value;
    }

    private Double parseNonNegative(String raw, String fieldName) {
        Double value = parseDouble(raw, fieldName);
        if (value == null) return null;
        if (value < 0) {
            showError("Valore non valido", fieldName + " non può essere negativo.");
            return null;
        }
        return value;
    }


    private Integer parsePositiveInt(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            showError("Campo obbligatorio mancante", fieldName + " è obbligatorio.");
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                showError("Valore non valido", fieldName + " deve essere maggiore di 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            showError("Formato numerico non valido", fieldName + " deve essere un intero valido.");
            return null;
        }
    }

    private Double parseDouble(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            showError("Campo obbligatorio mancante", fieldName + " è obbligatorio.");
            return null;
        }

        try {
            String normalized = raw.trim().replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            showError("Formato numerico non valido", fieldName + " deve essere un numero valido.");
            return null;
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
