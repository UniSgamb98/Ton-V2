package com.orodent.tonv2.features.cubage.creation.controller;

import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import javafx.collections.FXCollections;

public class CubageCreationController {

    private static final String SHOW_LEGACY_TEXT = "Seleziona Payload Legacy";
    private static final String HIDE_LEGACY_TEXT = "Nascondi Payload Legacy";

    private final CubageCreationView view;
    private final CubageCreationService service;

    public CubageCreationController(CubageCreationView view,
                                    CubageCreationService service) {
        this.view = view;
        this.service = service;

        initialize();
    }

    private void initialize() {
        view.setInfoText(service.getIntroMessage());

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

        if (!view.getPayloadSelector().getItems().isEmpty()) {
            view.getPayloadSelector().getSelectionModel().selectFirst();
        }
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
