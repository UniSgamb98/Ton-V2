package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.form.ConfirmUnsavedChangesDialog;
import com.orodent.tonv2.core.ui.form.DirtyStateTracker;
import com.orodent.tonv2.core.ui.form.FieldParsers;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelDraftDataService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import javafx.scene.control.Alert;

import java.util.List;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final LaboratoryNavigator navigator;
    private final CreateDiskModelService service;
    private final EditorMode editorMode;
    private final DiskModelDraftDataService draftDataService;
    private final DirtyStateTracker dirtyStateTracker;

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
        this.draftDataService = new DiskModelDraftDataService();
        this.dirtyStateTracker = new DirtyStateTracker()
                .track("code", () -> normalize(view.getCode()))
                .track("diameter", () -> normalize(view.getDiameter()))
                .track("superior", () -> normalize(view.getSuperiorOvermaterial()))
                .track("inferior", () -> normalize(view.getInferiorOvermaterial()))
                .track("pressure", () -> normalize(view.getPressure()))
                .track("gramsPerMm", () -> normalize(view.getGramsPerMm()))
                .track("numLayers", () -> normalize(view.getNumLayers()))
                .track("layerSignature", () -> draftDataService.buildLayerSignature(view.getLayerPercentageDrafts()))
                .track("rangeSignature", () -> draftDataService.buildRangeSignature(view.getRangeDrafts()));

        view.configureEditMode(editorMode.sourceBlankModelId() != null);
        setupActions();
        dirtyStateTracker.captureInitialState();
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
        dirtyStateTracker.captureInitialState();
    }

    private void navigateBackWithConfirmation() {
        if (!dirtyStateTracker.hasUnsavedChanges()) {
            navigator.showLaboratoryDiskModelArchive();
            return;
        }

        ConfirmUnsavedChangesDialog.UserChoice choice = ConfirmUnsavedChangesDialog.show(
                "Modifiche non salvate",
                "Vuoi salvare le modifiche prima di tornare all'archivio?",
                "Se scegli 'Non salvare' perderai le modifiche effettuate.",
                "Salva e torna"
        );

        if (choice == ConfirmUnsavedChangesDialog.UserChoice.SAVE) {
            save(false);
            return;
        }

        if (choice == ConfirmUnsavedChangesDialog.UserChoice.DISCARD) {
            navigator.showLaboratoryDiskModelArchive();
        }
    }

    private boolean save(boolean navigateAfterSave) {
        try {
            CreateDiskModelService.CreateDiskModelData modelData = new CreateDiskModelService.CreateDiskModelData(
                    FieldParsers.trimToNull(view.getCode()),
                    FieldParsers.parseDouble(view.getDiameter(), "Diametro"),
                    FieldParsers.parseDouble(view.getSuperiorOvermaterial(), "Overmaterial superiore default"),
                    FieldParsers.parseDouble(view.getInferiorOvermaterial(), "Overmaterial inferiore default"),
                    FieldParsers.parseDouble(view.getPressure(), "Pressione"),
                    FieldParsers.parseDouble(view.getGramsPerMm(), "Grammi per mm"),
                    FieldParsers.parseInteger(view.getNumLayers(), "Numero strati")
            );

            List<CreateDiskModelService.LayerData> layers = draftDataService.parseLayers(view.getLayerPercentageDrafts());
            List<CreateDiskModelService.HeightRangeData> ranges = draftDataService.parseRanges(view.getRangeDrafts());

            if (editorMode.sourceBlankModelId() == null) {
                service.createDiskModel(modelData, layers, ranges);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setHeaderText("Modello disco salvato");
                ok.setContentText("Il nuovo modello è stato registrato correttamente.");
                ok.showAndWait();

                dirtyStateTracker.captureInitialState();
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

                dirtyStateTracker.captureInitialState();
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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
