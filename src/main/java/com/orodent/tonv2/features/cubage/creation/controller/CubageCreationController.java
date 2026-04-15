package com.orodent.tonv2.features.cubage.creation.controller;

import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.service.CubageFormulaSetPersistenceService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import javafx.collections.FXCollections;

public class CubageCreationController {

    private static final String SHOW_LEGACY_TEXT = "Seleziona Payload Legacy";
    private static final String HIDE_LEGACY_TEXT = "Nascondi Payload Legacy";

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
        CubageCreationService.FormulaValidationResult validationResult = service.validateAndBuildFormulaSet(
                view.getCalculationSetNameField().getText(),
                view.getFormulaBuilderText(),
                payload
        );

        if (!validationResult.valid()) {
            view.setResultsText(validationResult.message());
            return;
        }

        try {
            CubageFormulaSetPersistenceService.SaveResult saveResult = persistenceService.save(validationResult.compilation());
            view.setResultsText(validationResult.message() + "\n\nSalvataggio completato: set #" + saveResult.formulaSetId()
                    + " versione v" + saveResult.version());
        } catch (RuntimeException e) {
            view.setResultsText("Errore salvataggio set di calcolo: " + e.getMessage());
        }
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
