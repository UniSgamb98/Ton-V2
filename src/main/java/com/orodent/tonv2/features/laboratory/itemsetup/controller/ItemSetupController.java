package com.orodent.tonv2.features.laboratory.itemsetup.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class ItemSetupController {

    private final ItemSetupView view;
    private final ItemSetupService service;
    private final DebouncedTaskRunner<List<Product>> productsLoader;
    private final DebouncedTaskRunner<Integer> activationRunner;
    private final DebouncedTaskRunner<Item> creationRunner;
    private final DebouncedTaskRunner<ItemSetupService.ProductSetupStatus> productSetupRunner;

    public ItemSetupController(ItemSetupView view,
                               ItemSetupService service,
                               Executor backgroundExecutor) {
        this.view = view;
        this.service = service;
        this.productsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.activationRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.creationRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.productSetupRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(100));

        setupActions();
    }

    public void loadInitialData() {
        productsLoader.runNow(
                service::findAllProduct,
                view::showProductsLoading,
                view::showProducts,
                error -> view.showLoadError("Errore durante il caricamento dei prodotti.")
        );
    }

    private void setupActions() {
        view.getActivateLatestCompositionButton().setOnAction(e -> activateLatestComposition());
        view.getCreateItemButton().setOnAction(e -> createItem());
        view.getProductSelector().valueProperty().addListener((obs, oldProduct, newProduct) -> {
            if (newProduct == null) {
                view.showProductSetup(ItemSetupService.ProductSetupStatus.empty());
                return;
            }
            loadProductSetup(newProduct);
        });
    }

    private void loadProductSetup(Product product) {
        productSetupRunner.runDebounced(
                () -> service.findProductSetupStatus(product.id()),
                view::showProductSetupLoading,
                view::showProductSetup,
                error -> view.showLoadError("Errore durante la verifica della configurazione prodotto.")
        );
    }

    private void activateLatestComposition() {
        try {
            Product product = requireProduct();
            activationRunner.runNow(
                    () -> service.activateLatestComposition(product.id()),
                    view::showActivationLoading,
                    compositionId -> {
                        view.showSuccess("Composizione #" + compositionId + " attivata con successo.");
                        loadProductSetup(product);
                    },
                    error -> view.showLoadError(error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Errore durante l'aggiornamento della composizione attiva.")
            );
        } catch (IllegalArgumentException ex) {
            view.showLoadError(ex.getMessage());
        }
    }

    private void createItem() {
        try {
            Product product = requireProduct();
            String itemCode = parseItemCode(view.getItemCodeField().getText());
            double heightMm = parseHeight(view.getHeightField().getText());

            creationRunner.runNow(
                    () -> service.createItemForActiveComposition(itemCode, product.id(), heightMm),
                    view::showCreationLoading,
                    item -> view.showSuccess("Articolo pronto: " + item.code() + " (id " + item.id() + ")"),
                    error -> view.showLoadError(error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Errore durante la creazione item.")
            );
        } catch (IllegalArgumentException ex) {
            view.showLoadError(ex.getMessage());
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

    public void dispose() {
        productsLoader.cancel();
        activationRunner.cancel();
        creationRunner.cancel();
        productSetupRunner.cancel();
    }
}
