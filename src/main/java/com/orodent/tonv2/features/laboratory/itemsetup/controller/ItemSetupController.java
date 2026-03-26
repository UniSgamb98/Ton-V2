package com.orodent.tonv2.features.laboratory.itemsetup.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;

public class ItemSetupController {

    private final ItemSetupView view;
    private final ItemSetupService service;

    public ItemSetupController(ItemSetupView view,
                               ItemSetupService service) {
        this.view = view;
        this.service = service;

        loadData();
        setupActions();
    }

    private void loadData() {
        view.getProductSelector().getItems().setAll(service.findAllProduct());
    }

    private void setupActions() {
        view.getActivateLatestCompositionButton().setOnAction(e -> activateLatestComposition());
        view.getCreateItemButton().setOnAction(e -> createItem());
    }

    private void activateLatestComposition() {
        try {
            Product product = requireProduct();
            int compositionId = service.activateLatestComposition(product.id());
            view.setFeedback("Composizione ID:" + compositionId + "attivata con successo.", false);
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante l'aggiornamento della composizione attiva.", true);
        }
    }

    private void createItem() {
        try {
            Product product = requireProduct();
            String itemCode = parseItemCode(view.getItemCodeField().getText());
            double heightMm = parseHeight(view.getHeightField().getText());

            Item item = service.createItemForActiveComposition(itemCode, product.id(), heightMm);
            view.setFeedback("Item pronto: " + item.code() + " (id " + item.id() + ")", false);
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante la creazione item.", true);
        }
    }

    private Product requireProduct() {
        Product product = view.getProductSelector().getValue();
        if (product == null) {
            throw new IllegalArgumentException("Seleziona un prodotto.");
        }
        return product;
    }

    private String parseItemCode(String raw) {
        String code = raw == null ? "" : raw.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Codice item obbligatorio.");
        }
        return code;
    }

    private double parseHeight(String raw) {
        try {
            return Double.parseDouble((raw == null ? "" : raw.trim()).replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Altezza non valida.");
        }
    }
}
