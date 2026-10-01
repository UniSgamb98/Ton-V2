package com.orodent.tonv2.features.cubage.creation.controller;

import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.service.CubageFormulaSetPersistenceService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class CubageCreationController {

    private static final String SHOW_LEGACY_TEXT = "Seleziona Payload Legacy";
    private static final String HIDE_LEGACY_TEXT = "Nascondi Payload Legacy";
    private static final String NEW_SET_OPTION = "➕ Nuovo set di calcolo";

    private final CubageCreationView view;
    private final CubageCreationService service;
    private final CubageFormulaSetPersistenceService persistenceService;
    private final DebouncedTaskRunner<InitialData> initialDataLoader;
    private final DebouncedTaskRunner<List<CubageCreationService.PayloadOption>> legacyPayloadLoader;
    private final DebouncedTaskRunner<String> previewLoader;
    private final DebouncedTaskRunner<SaveOutcome> saveRunner;

    public CubageCreationController(CubageCreationView view,
                                    CubageCreationService service,
                                    CubageFormulaSetPersistenceService persistenceService,
                                    Executor executor) {
        this.view = view;
        this.service = service;
        this.persistenceService = persistenceService;
        this.initialDataLoader = new DebouncedTaskRunner<>(executor, Duration.ZERO);
        this.legacyPayloadLoader = new DebouncedTaskRunner<>(executor, Duration.millis(100));
        this.previewLoader = new DebouncedTaskRunner<>(executor, Duration.millis(100));
        this.saveRunner = new DebouncedTaskRunner<>(executor, Duration.ZERO);
        setupActions();
    }

    public void loadInitialData() {
        initialDataLoader.runNow(
                () -> new InitialData(
                        persistenceService.loadFormulaSetCodes(),
                        service.getLatestPayloadOptions()
                ),
                view::showInitialLoading,
                this::applyInitialData,
                error -> view.showLoadError("Errore durante il caricamento dei dati di cubaggio.")
        );
    }

    private void applyInitialData(InitialData data) {
        setCalculationSetOptions(data.formulaSetCodes());
        view.setPayloadOptions(FXCollections.observableArrayList(data.payloadOptions()));
        view.showLoadSuccess();
        if (!data.payloadOptions().isEmpty()) {
            view.getPayloadSelector().getSelectionModel().selectFirst();
        }
    }

    private void setupActions() {
        view.getPayloadSelector().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            loadLegacyOptionsFor(newValue);
            if (!isLegacySelectionVisible()) {
                loadPreview(newValue, false);
            }
        });

        view.getLegacyPayloadSelector().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (isLegacySelectionVisible() && newValue != null) {
                loadPreview(newValue, true);
            }
        });

        view.getSelectLegacyPayloadButton().setOnAction(event -> toggleLegacySelection());
        view.getSaveCalculationSetButton().setOnAction(event -> saveFormulaSet());
    }

    private void loadLegacyOptionsFor(CubageCreationService.PayloadOption selectedPayload) {
        if (selectedPayload == null) {
            legacyPayloadLoader.cancel();
            view.setLegacyPayloadOptions(FXCollections.observableArrayList());
            return;
        }
        legacyPayloadLoader.runDebounced(
                () -> service.getAllVersionsForPayload(selectedPayload.payloadCode()),
                view::showPayloadLoading,
                values -> {
                    view.setLegacyPayloadOptions(FXCollections.observableArrayList(values));
                    view.showLoadSuccess();
                    if (!values.isEmpty()) {
                        view.getLegacyPayloadSelector().getSelectionModel().selectLast();
                    }
                },
                error -> view.showLoadError("Errore durante il caricamento delle versioni payload.")
        );
    }

    private void loadPreview(CubageCreationService.PayloadOption option, boolean legacy) {
        if (option == null) {
            previewLoader.cancel();
            view.setPayloadPreviewText("Nessun payload selezionato.");
            return;
        }
        previewLoader.runDebounced(
                () -> service.buildPayloadPreview(option, legacy),
                view::showPayloadLoading,
                preview -> {
                    view.setPayloadPreviewText(preview);
                    view.showLoadSuccess();
                },
                error -> view.showLoadError("Errore durante il caricamento del payload.")
        );
    }

    private void saveFormulaSet() {
        CubageCreationService.PayloadOption payload = getCurrentSelectedPayload();
        String formulaSetName = resolveFormulaSetName();
        if (formulaSetName == null) {
            return;
        }
        String formulasText = view.getFormulaBuilderText();

        saveRunner.runNow(
                () -> save(formulaSetName, formulasText, payload),
                view::showSaving,
                this::applySaveOutcome,
                error -> view.showSaveError("Errore durante il salvataggio del set di calcolo.")
        );
    }

    private SaveOutcome save(String formulaSetName,
                             String formulasText,
                             CubageCreationService.PayloadOption payload) {
        CubageCreationService.FormulaValidationResult validation =
                service.validateAndBuildFormulaSet(formulaSetName, formulasText, payload);
        if (!validation.valid()) {
            return new SaveOutcome(formulaSetName, validation.message(), null, List.of(), false);
        }
        CubageFormulaSetPersistenceService.SaveResult saved = persistenceService.save(validation.compilation());
        return new SaveOutcome(formulaSetName, validation.message(), saved,
                persistenceService.loadFormulaSetCodes(), true);
    }

    private void applySaveOutcome(SaveOutcome outcome) {
        if (!outcome.saved()) {
            view.showValidationError(outcome.message());
            return;
        }
        setCalculationSetOptions(outcome.formulaSetCodes());
        view.getCalculationSetSelector().setValue(outcome.formulaSetName());
        CubageFormulaSetPersistenceService.SaveResult saved = outcome.saveResult();
        view.showSaveSuccess(outcome.message() + "\n\nSalvataggio completato: set #"
                + saved.formulaSetId() + " versione v" + saved.version());
    }

    private void setCalculationSetOptions(List<String> codes) {
        view.getCalculationSetSelector().getItems().setAll(codes);
        view.getCalculationSetSelector().getItems().add(0, NEW_SET_OPTION);
    }

    private String resolveFormulaSetName() {
        String selected = view.getCalculationSetSelector().getValue();
        if (selected == null || selected.isBlank() || NEW_SET_OPTION.equals(selected)) {
            return askNewFormulaSetName().orElse(null);
        }
        return selected;
    }

    private Optional<String> askNewFormulaSetName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuovo set di calcolo");
        dialog.setHeaderText("Inserisci il nome del set di calcolo");
        dialog.setContentText("Nome set:");
        dialog.getEditor().setPromptText("Nuovo set di calcolo");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        String value = result.get() == null ? "" : result.get().trim();
        if (value.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Nome set mancante");
            alert.setContentText("Inserisci un nome valido per il set di calcolo.");
            alert.showAndWait();
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private CubageCreationService.PayloadOption getCurrentSelectedPayload() {
        if (isLegacySelectionVisible()) {
            CubageCreationService.PayloadOption legacy = view.getLegacyPayloadSelector().getValue();
            if (legacy != null) {
                return legacy;
            }
        }
        return view.getPayloadSelector().getValue();
    }

    private void toggleLegacySelection() {
        if (isLegacySelectionVisible()) {
            view.setLegacySelectorVisible(false);
            view.setSelectLegacyPayloadButtonText(SHOW_LEGACY_TEXT);
            loadPreview(view.getPayloadSelector().getValue(), false);
            return;
        }
        view.setLegacySelectorVisible(true);
        view.setSelectLegacyPayloadButtonText(HIDE_LEGACY_TEXT);
        CubageCreationService.PayloadOption selectedLegacy = view.getLegacyPayloadSelector().getValue();
        if (selectedLegacy != null) {
            loadPreview(selectedLegacy, true);
        }
    }

    private boolean isLegacySelectionVisible() {
        return view.getLegacyPayloadSelector().isVisible();
    }

    public void dispose() {
        initialDataLoader.cancel();
        legacyPayloadLoader.cancel();
        previewLoader.cancel();
        saveRunner.cancel();
    }

    private record InitialData(List<String> formulaSetCodes,
                               List<CubageCreationService.PayloadOption> payloadOptions) {
    }

    private record SaveOutcome(String formulaSetName,
                               String message,
                               CubageFormulaSetPersistenceService.SaveResult saveResult,
                               List<String> formulaSetCodes,
                               boolean saved) {
    }
}
