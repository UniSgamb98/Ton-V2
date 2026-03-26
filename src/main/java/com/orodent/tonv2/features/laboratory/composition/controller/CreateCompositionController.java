package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.sql.SQLException;
import java.util.Optional;

public class CreateCompositionController {

    private static final Product NEW_PRODUCT_OPTION = new Product(-1, "➕ Nuovo prodotto", "");

    private final CreateCompositionView view;
    private final AppController app;
    private final CreateCompositionService service;

    public CreateCompositionController(CreateCompositionView view,
                                       AppController app,
                                       CreateCompositionService service) {
        this.view = view;
        this.app = app;
        this.service = service;

        loadProducts();
        loadBlankModels();
        loadPowders();
        setupActions();
    }

    private void loadProducts() {
        view.getProductSelector().getItems().setAll(service.findAllProducts());
        if (!view.getProductSelector().getItems().contains(NEW_PRODUCT_OPTION)) {
            view.getProductSelector().getItems().addFirst(NEW_PRODUCT_OPTION);
        }
    }

    private void loadPowders() {
        view.setAvailablePowders(service.findAllPowders());
    }

    private void loadBlankModels() {
        view.getBlankModelSelector().getItems().setAll(service.findAllBlankModels());
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> saveComposition());
        view.getProductSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                view.setLoadLatestVersionVisible(newValue != null && !isNewProductOption(newValue))
        );
        view.getBlankModelSelector().valueProperty().addListener((obs, oldModel, newModel) -> {
            if (newModel != null) {
                view.setLayerCount(newModel.numLayers());
            }
        });
        view.getLoadLatestVersionButton().setOnAction(e -> loadLatestVersion());
        view.setLoadLatestVersionVisible(false);
    }

    private void loadLatestVersion() {
        Product selectedProduct = view.getProductSelector().getValue();

        if (selectedProduct == null || isNewProductOption(selectedProduct)) {
            return;
        }

        Optional<CreateCompositionService.LatestCompositionData> latest = service.loadLatestComposition(selectedProduct.id());
        if (latest.isEmpty()) {
            showWarning("Nessuna versione trovata", "Non esiste ancora una composizione da caricare per questo prodotto.");
            return;
        }

        CreateCompositionService.LatestCompositionData data = latest.get();
        if (data.blankModelId() != null) {
            view.getBlankModelSelector().getItems().stream()
                    .filter(model -> model.id() == data.blankModelId())
                    .findFirst()
                    .ifPresent(view.getBlankModelSelector()::setValue);
        }

        view.setNotes(data.notes());
        view.replaceLayers(data.layerDrafts());
    }

    private void saveComposition() {
        Product product = resolveProduct();
        if (product == null) {
            return;
        }

        BlankModel blankModel = view.getBlankModelSelector().getValue();
        view.renumberLayers();

        try {
            service.saveComposition(new CreateCompositionService.SaveCompositionRequest(
                    product,
                    blankModel,
                    view.getLayers(),
                    view.getNotes()
            ));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Composizione Salvata");
            alert.setContentText("La ricetta è stata registrata correttamente.");
            alert.showAndWait();

            app.showLaboratory();
        } catch (IllegalArgumentException ex) {
            showWarning("Validazione dati", ex.getMessage());
        } catch (RuntimeException ex) {
            showDbError("Errore salvataggio composizione", ex);
        }
    }

    private Product resolveProduct() {
        Product selected = view.getProductSelector().getValue();

        if (selected != null && !isNewProductOption(selected)) {
            return selected;
        }

        return showMissingProductDialog().orElse(null);
    }

    private boolean isNewProductOption(Product product) {
        return product != null && product.id() == NEW_PRODUCT_OPTION.id();
    }

    private Optional<Product> showMissingProductDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuovo prodotto");
        dialog.setHeaderText("Product selector vuoto");
        dialog.setContentText("Codice nuovo prodotto:");

        dialog.getEditor().setPromptText("Inserisci codice prodotto");

        Label helper = new Label(
                "Stai creando una composizione per un nuovo prodotto.\n" +
                        "Se vuoi creare una nuova versione di un prodotto esistente, " +
                        "premi Annulla, torna indietro e seleziona il prodotto nel Product selector."
        );
        helper.setWrapText(true);
        dialog.getDialogPane().setExpandableContent(helper);
        dialog.getDialogPane().setExpanded(true);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        try {
            Product newProduct = service.createProduct(result.get());
            view.getProductSelector().getItems().add(newProduct);
            view.getProductSelector().getItems().remove(NEW_PRODUCT_OPTION);
            view.getProductSelector().getItems().addFirst(NEW_PRODUCT_OPTION);
            view.getProductSelector().setValue(newProduct);
            return Optional.of(newProduct);
        } catch (IllegalArgumentException ex) {
            showWarning("Codice prodotto mancante", ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException e) {
            showDbError("Errore creazione prodotto", e);
            return Optional.empty();
        }
    }

    private void showWarning(String header, String content) {
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setHeaderText(header);
        warning.setContentText(content);
        warning.showAndWait();
    }

    private void showDbError(String header, RuntimeException ex) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }

        String message = ex.getMessage();
        if (cause instanceof SQLException sqlEx && "45000".equals(sqlEx.getSQLState())) {
            message = sqlEx.getMessage();
        }

        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setHeaderText(header);
        error.setContentText(message);
        error.showAndWait();
    }
}
