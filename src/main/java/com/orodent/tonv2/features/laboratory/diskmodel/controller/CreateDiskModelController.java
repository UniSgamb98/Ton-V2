package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.form.ConfirmUnsavedChangesDialog;
import com.orodent.tonv2.core.ui.form.DirtyStateTracker;
import com.orodent.tonv2.core.ui.form.FieldParsers;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelDraftDataService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import javafx.scene.control.Alert;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class CreateDiskModelController {

    private final CreateDiskModelView view;
    private final LaboratoryNavigator navigator;
    private final CreateDiskModelService service;
    private final EditorMode editorMode;
    private final DiskModelDraftDataService draftDataService;
    private final DirtyStateTracker dirtyStateTracker;
    private final DiskModelArchiveService archiveService;
    private final DebouncedTaskRunner<DiskModelArchiveService.DiskModelSnapshot> initialDataLoader;
    private final DebouncedTaskRunner<SaveResult> saveRunner;

    public CreateDiskModelController(CreateDiskModelView view,
                                     LaboratoryNavigator navigator,
                                     CreateDiskModelService service,
                                     DiskModelArchiveService archiveService,
                                     Executor backgroundExecutor) {
        this(view, navigator, service, archiveService, EditorMode.create(), backgroundExecutor);
    }

    public CreateDiskModelController(CreateDiskModelView view,
                                     LaboratoryNavigator navigator,
                                     CreateDiskModelService service,
                                     DiskModelArchiveService archiveService,
                                     EditorMode editorMode,
                                     Executor backgroundExecutor) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.editorMode = editorMode;
        this.archiveService = archiveService;
        this.initialDataLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.saveRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
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
        if (editorMode.sourceBlankModelId() == null) {
            dirtyStateTracker.captureInitialState();
        }
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
            save(true);
            return;
        }

        if (choice == ConfirmUnsavedChangesDialog.UserChoice.DISCARD) {
            navigator.showLaboratoryDiskModelArchive();
        }
    }

    public void loadInitialData() {
        if (editorMode.sourceBlankModelId() == null) {
            return;
        }
        initialDataLoader.runNow(
                () -> archiveService.loadDiskModelSnapshot(editorMode.sourceBlankModelId()),
                view::showInitialLoading,
                this::applySnapshot,
                error -> view.showLoadError("Impossibile caricare il modello disco.")
        );
    }

    private void applySnapshot(DiskModelArchiveService.DiskModelSnapshot snapshot) {
        if (snapshot == null) {
            view.showLoadError("Modello disco non trovato.");
            return;
        }
        view.fillFromModel(
                snapshot.model().code(), snapshot.model().diameterMm(),
                snapshot.model().superiorOvermaterialDefaultMm(), snapshot.model().inferiorOvermaterialDefaultMm(),
                snapshot.model().pressureKgCm2(), snapshot.model().gramsPerMm(), snapshot.model().numLayers(),
                snapshot.layers().stream().map(layer -> layer.diskPercentage()).toList(),
                snapshot.ranges().stream().map(range -> new CreateDiskModelView.HeightRangeDraft(
                        String.valueOf(range.minHeightMm()), String.valueOf(range.maxHeightMm()),
                        String.valueOf(range.superiorOvermaterialMm()), String.valueOf(range.inferiorOvermaterialMm())
                )).toList()
        );
        dirtyStateTracker.captureInitialState();
        view.showLoadSuccess();
    }

    private void save(boolean navigateAfterSave) {
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

            saveRunner.runNow(
                    () -> saveInBackground(modelData, layers, ranges),
                    view::showSaving,
                    result -> showSaveSuccess(result, navigateAfterSave),
                    error -> view.showLoadError("Non è stato possibile salvare il modello disco: " + error.getMessage())
            );
        } catch (IllegalArgumentException ex) {
            showError("Validazione dati", ex.getMessage());
        }
    }

    private SaveResult saveInBackground(CreateDiskModelService.CreateDiskModelData modelData,
                                        List<CreateDiskModelService.LayerData> layers,
                                        List<CreateDiskModelService.HeightRangeData> ranges) {
        if (editorMode.sourceBlankModelId() == null) {
            return new SaveResult(service.createDiskModel(modelData, layers, ranges).id(), 0, false);
        }
        CreateDiskModelService.VersionedSaveResult result = service.createDiskModelVersionFrom(
                editorMode.sourceBlankModelId(), modelData, layers, ranges);
        return new SaveResult(result.newBlankModelId(), result.copiedCompositionAssociations(), true);
    }

    private void showSaveSuccess(SaveResult result, boolean navigateAfterSave) {
        view.showLoadSuccess();
        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setHeaderText(result.versioned() ? "Modifiche salvate" : "Modello disco salvato");
        ok.setContentText(result.versioned()
                ? "Creato nuovo modello disco (ID " + result.modelId() + ") e copiate "
                        + result.copiedAssociations() + " associazioni composizione."
                : "Il nuovo modello è stato registrato correttamente.");
        ok.showAndWait();
        dirtyStateTracker.captureInitialState();
        if (navigateAfterSave) {
            if (result.versioned()) navigator.showLaboratoryDiskModelArchive();
            else navigator.showLaboratory();
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

    public void dispose() {
        initialDataLoader.cancel();
        saveRunner.cancel();
    }

    private record SaveResult(int modelId, int copiedAssociations, boolean versioned) {
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
