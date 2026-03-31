package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final LaboratoryNavigator navigator;
    private final CreateDiskModelService service;
    private final EditorMode editorMode;

    private String initialCode;
    private String initialDiameter;
    private String initialSuperior;
    private String initialInferior;
    private String initialPressure;
    private String initialGramsPerMm;
    private String initialNumLayers;
    private String initialLayerSignature;
    private String initialRangeSignature;

    public CreateDiskModelController(CreateDiskModelView view,
                                     LaboratoryNavigator navigator,
                                     CreateDiskModelService service) {
        this(view, navigator, service, EditorMode.create());
    }

    public CreateDiskModelController(CreateDiskModelView view,
                                     LaboratoryNavigator navigator,
                                     CreateDiskModelService service,
                                     EditorMode editorMode) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.editorMode = editorMode;

        view.configureEditMode(editorMode.sourceBlankModelId() != null);
        setupActions();
        captureInitialState();
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> save(true));
        view.getBackButton().setOnAction(e -> {
            if (editorMode.sourceBlankModelId() != null) {
                navigateBackWithConfirmation();
            }
        });
    }

    public void markAsClean() {
        captureInitialState();
    }

    private void navigateBackWithConfirmation() {
        if (!hasUnsavedChanges()) {
            navigator.showLaboratoryDiskModelArchive();
            return;
        }

        ButtonType saveAndBack = new ButtonType("Salva e torna", ButtonBar.ButtonData.YES);
        ButtonType discardAndBack = new ButtonType("Non salvare", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Modifiche non salvate");
        alert.setHeaderText("Vuoi salvare le modifiche prima di tornare all'archivio?");
        alert.setContentText("Se scegli 'Non salvare' perderai le modifiche effettuate.");
        alert.getButtonTypes().setAll(saveAndBack, discardAndBack, cancel);

        ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == saveAndBack) {
            save(false);
            return;
        }

        if (result == discardAndBack) {
            navigator.showLaboratoryDiskModelArchive();
        }
    }

    private boolean save(boolean navigateAfterSave) {
        try {
            CreateDiskModelService.CreateDiskModelData modelData = new CreateDiskModelService.CreateDiskModelData(
                    trimToNull(view.getCode()),
                    parseDouble(view.getDiameter(), "Diametro"),
                    parseDouble(view.getSuperiorOvermaterial(), "Overmaterial superiore default"),
                    parseDouble(view.getInferiorOvermaterial(), "Overmaterial inferiore default"),
                    parseDouble(view.getPressure(), "Pressione"),
                    parseDouble(view.getGramsPerMm(), "Grammi per mm"),
                    parseInteger(view.getNumLayers())
            );

            List<CreateDiskModelService.LayerData> layers = parseLayers();
            List<CreateDiskModelService.HeightRangeData> ranges = parseRanges();

            if (editorMode.sourceBlankModelId() == null) {
                service.createDiskModel(modelData, layers, ranges);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setHeaderText("Modello disco salvato");
                ok.setContentText("Il nuovo modello è stato registrato correttamente.");
                ok.showAndWait();

                captureInitialState();
                if (navigateAfterSave) {
                    navigator.showLaboratory();
                }
            } else {
                CreateDiskModelService.VersionedSaveResult result = service.createDiskModelVersionFrom(
                        editorMode.sourceBlankModelId(),
                        modelData,
                        layers,
                        ranges
                );

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setHeaderText("Modifiche salvate");
                ok.setContentText("Creato nuovo modello disco (ID " + result.newBlankModelId()
                        + ") e copiate " + result.copiedCompositionAssociations() + " associazioni composizione.");
                ok.showAndWait();

                captureInitialState();
                if (navigateAfterSave) {
                    navigator.showLaboratoryDiskModelArchive();
                }
            }
            return true;
        } catch (IllegalArgumentException ex) {
            showError("Validazione dati", ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            showError("Errore salvataggio modello", "Non è stato possibile salvare il modello disco: " + ex.getMessage());
            return false;
        }
    }

    private boolean hasUnsavedChanges() {
        return !normalize(view.getCode()).equals(initialCode)
                || !normalize(view.getDiameter()).equals(initialDiameter)
                || !normalize(view.getSuperiorOvermaterial()).equals(initialSuperior)
                || !normalize(view.getInferiorOvermaterial()).equals(initialInferior)
                || !normalize(view.getPressure()).equals(initialPressure)
                || !normalize(view.getGramsPerMm()).equals(initialGramsPerMm)
                || !normalize(view.getNumLayers()).equals(initialNumLayers)
                || !buildLayerSignature().equals(initialLayerSignature)
                || !buildRangeSignature().equals(initialRangeSignature);
    }

    private void captureInitialState() {
        initialCode = normalize(view.getCode());
        initialDiameter = normalize(view.getDiameter());
        initialSuperior = normalize(view.getSuperiorOvermaterial());
        initialInferior = normalize(view.getInferiorOvermaterial());
        initialPressure = normalize(view.getPressure());
        initialGramsPerMm = normalize(view.getGramsPerMm());
        initialNumLayers = normalize(view.getNumLayers());
        initialLayerSignature = buildLayerSignature();
        initialRangeSignature = buildRangeSignature();
    }

    private String buildLayerSignature() {
        StringBuilder sb = new StringBuilder();
        view.getLayerPercentageDrafts().forEach(layer -> sb
                .append(layer.layerNumber())
                .append('=')
                .append(normalize(layer.percentage()))
                .append(';'));
        return sb.toString();
    }

    private String buildRangeSignature() {
        StringBuilder sb = new StringBuilder();
        view.getRangeDrafts().forEach(range -> sb
                .append(normalize(range.minHeight()))
                .append('|')
                .append(normalize(range.maxHeight()))
                .append('|')
                .append(normalize(range.superiorOvermaterial()))
                .append('|')
                .append(normalize(range.inferiorOvermaterial()))
                .append(';'));
        return sb.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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

    private Integer parseInteger(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Numero strati" + " deve essere un intero valido.");
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

    public record EditorMode(Integer sourceBlankModelId) {
        public static EditorMode create() {
            return new EditorMode(null);
        }

        public static EditorMode edit(Integer sourceBlankModelId) {
            return new EditorMode(sourceBlankModelId);
        }
    }
}
