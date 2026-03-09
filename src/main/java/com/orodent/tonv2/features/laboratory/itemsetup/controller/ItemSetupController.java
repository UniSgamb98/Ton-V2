package com.orodent.tonv2.features.laboratory.itemsetup.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;

public class ItemSetupController {

    private final ItemSetupView view;
    private final ProductRepository productRepo;
    private final ItemRepository itemRepo;
    private final CompositionRepository compositionRepo;
    private final ItemSetupService service;

    public ItemSetupController(ItemSetupView view,
                               ProductRepository productRepo,
                               ItemRepository itemRepo,
                               CompositionRepository compositionRepo,
                               ItemSetupService service) {
        this.view = view;
        this.productRepo = productRepo;
        this.itemRepo = itemRepo;
        this.compositionRepo = compositionRepo;
        this.service = service;

        loadData();
        setupActions();
    }

    private void loadData() {
        view.getProductSelector().getItems().setAll(productRepo.findAll());
    }

    private void setupActions() {
        view.getProductSelector().valueProperty().addListener((obs, oldVal, newVal) -> refreshActiveCompositionLabel());
        view.getActivateLatestCompositionButton().setOnAction(e -> activateLatestComposition());
        view.getCreateItemButton().setOnAction(e -> createItem());
    }

    private void refreshActiveCompositionLabel() {
        Product product = view.getProductSelector().getValue();
        if (product == null) {
            view.setActiveCompositionText("Composizione attiva: -");
            return;
        }

        compositionRepo.findActiveCompositionId(product.id())
                .ifPresentOrElse(
                        id -> view.setActiveCompositionText("Composizione attiva: #" + id),
                        () -> view.setActiveCompositionText("Composizione attiva: nessuna")
                );
    }

    private void activateLatestComposition() {
        try {
            Product product = requireProduct();
            int compositionId = service.activateLatestComposition(product.id(), compositionRepo);
            view.setActiveCompositionText("Composizione attiva: " + compositionId);
            view.setFeedback("Composizione attiva aggiornata con successo.", false);
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

            Item item = service.createItemForActiveComposition(itemCode, product.id(), heightMm, itemRepo, compositionRepo);
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
