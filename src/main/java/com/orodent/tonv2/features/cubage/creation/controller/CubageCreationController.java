package com.orodent.tonv2.features.cubage.creation.controller;

import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.service.CubageFormulaSetPersistenceService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class CubageCreationController {

    private static final String SHOW_LEGACY_TEXT = "Seleziona Payload Legacy";
    private static final String HIDE_LEGACY_TEXT = "Nascondi Payload Legacy";
    private static final String NEW_SET_OPTION = "➕ Nuovo set di calcolo";

    private final CubageCreationView view;
    private final CubageCreationService service;
    private final CubageFormulaSetPersistenceService persistenceService;

    public CubageCreationController(CubageCreationView view,
                                    CubageCreationService service,
                                    CubageFormulaSetPersistenceService persistenceService) {
        this.view = view;
        this.service = service;
        this.persistenceService = persistenceService;

        initialize();
    }

    private void initialize() {
        loadCalculationSetSelector();
        view.setPayloadOptions(FXCollections.observableArrayList(service.getLatestPayloadOptions()));
        view.getPayloadSelector().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            populateLegacyOptionsFor(newValue);
            if (!isLegacySelectionVisible()) {
                updatePreviewForActiveSelection();
            }
        });

        view.getLegacyPayloadSelector().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (isLegacySelectionVisible() && newValue != null) {
                view.setPayloadPreviewText(service.buildPayloadPreview(newValue, true));
            }
        });

        view.getSelectLegacyPayloadButton().setOnAction(event -> toggleLegacySelection());
        view.getSaveCalculationSetButton().setOnAction(event -> saveFormulaSet());

        if (!view.getPayloadSelector().getItems().isEmpty()) {
            view.getPayloadSelector().getSelectionModel().selectFirst();
        }
    }

    private void saveFormulaSet() {
        CubageCreationService.PayloadOption payload = getCurrentSelectedPayload();
        String formulaSetName = resolveFormulaSetName();
        if (formulaSetName == null) {
            return;
        }

        CubageCreationService.FormulaValidationResult validationResult;
        try {
            validationResult = service.validateAndBuildFormulaSet(
                    formulaSetName,
                    view.getFormulaBuilderText(),
                    payload
            );
        } catch (RuntimeException e) {
            view.setResultsText("Errore validazione set di calcolo: " + e.getMessage());
            return;
        }

        if (!validationResult.valid()) {
            view.setResultsText(validationResult.message());
            return;
        }

        try {
            CubageFormulaSetPersistenceService.SaveResult saveResult = persistenceService.save(validationResult.compilation());
            view.setResultsText(validationResult.message() + "\n\nSalvataggio completato: set #" + saveResult.formulaSetId()
                    + " versione v" + saveResult.version());
            loadCalculationSetSelector();
            view.getCalculationSetSelector().setValue(formulaSetName);
        } catch (RuntimeException e) {
            view.setResultsText("Errore salvataggio set di calcolo: " + e.getMessage());
        }
    }

    private void loadCalculationSetSelector() {
        view.getCalculationSetSelector().getItems().setAll(persistenceService.loadFormulaSetCodes());
        if (!view.getCalculationSetSelector().getItems().contains(NEW_SET_OPTION)) {
            view.getCalculationSetSelector().getItems().add(0, NEW_SET_OPTION);
        }
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
            CubageCreationService.PayloadOption legacy = view.getLegacyPayloadSelector().getSelectionModel().getSelectedItem();
            if (legacy != null) {
                return legacy;
            }
        }
        return view.getPayloadSelector().getSelectionModel().getSelectedItem();
    }

    private void populateLegacyOptionsFor(CubageCreationService.PayloadOption selectedActivePayload) {
        if (selectedActivePayload == null) {
            view.setLegacyPayloadOptions(FXCollections.observableArrayList());
            return;
        }

        view.setLegacyPayloadOptions(FXCollections.observableArrayList(
                service.getAllVersionsForPayload(selectedActivePayload.payloadCode())
        ));

        if (!view.getLegacyPayloadSelector().getItems().isEmpty()) {
            view.getLegacyPayloadSelector().getSelectionModel().selectLast();
        }
    }

    private void toggleLegacySelection() {
        boolean currentlyVisible = isLegacySelectionVisible();
        if (currentlyVisible) {
            hideLegacySelector();
            updatePreviewForActiveSelection();
            return;
        }

        showLegacySelector();
        CubageCreationService.PayloadOption selectedLegacy = view.getLegacyPayloadSelector()
                .getSelectionModel()
                .getSelectedItem();
        if (selectedLegacy != null) {
            view.setPayloadPreviewText(service.buildPayloadPreview(selectedLegacy, true));
        }
    }

    private void showLegacySelector() {
        view.setLegacySelectorVisible(true);
        view.setSelectLegacyPayloadButtonText(HIDE_LEGACY_TEXT);
    }

    private void hideLegacySelector() {
        view.setLegacySelectorVisible(false);
        view.setSelectLegacyPayloadButtonText(SHOW_LEGACY_TEXT);
    }

    private void updatePreviewForActiveSelection() {
        CubageCreationService.PayloadOption selected = view.getPayloadSelector().getSelectionModel().getSelectedItem();
        view.setPayloadPreviewText(service.buildPayloadPreview(selected, false));
    }

    private boolean isLegacySelectionVisible() {
        return view.getLegacyPayloadSelector().isVisible();
    }
}
