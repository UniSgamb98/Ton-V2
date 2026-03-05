package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.view.CreateCompositionView;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class CreateCompositionController {

    private static final Product NEW_PRODUCT_OPTION = new Product(-1, "➕ Nuovo prodotto", "");

    private final CreateCompositionView view;
    private final AppController app;
    private final PowderRepository powderRepo;
    private final CompositionRepository compositionRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final ProductRepository productRepo;
    private final BlankModelRepository blankModelRepo;

    public CreateCompositionController(CreateCompositionView view, AppController app, PowderRepository powderRepo, CompositionRepository compositionRepo, CompositionLayerIngredientRepository compositionLayerIngredientRepo, ProductRepository productRepo, BlankModelRepository blankModelRepo) {
        this.view = view;
        this.app = app;
        this.powderRepo = powderRepo;
        this.compositionRepo = compositionRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.productRepo = productRepo;
        this.blankModelRepo = blankModelRepo;

        loadProducts();
        loadBlankModels();
        loadPowders();
        setupActions();
    }

    private void loadProducts() {
        view.getProductSelector().getItems().setAll(productRepo.findAll());
        if (!view.getProductSelector().getItems().contains(NEW_PRODUCT_OPTION)) {
            view.getProductSelector().getItems().add(0, NEW_PRODUCT_OPTION);
        }
    }

    private void loadPowders() {
        view.setAvailablePowders(powderRepo.findAll());
    }

    private void loadBlankModels() {
        view.getBlankModelSelector().getItems().setAll(blankModelRepo.findAll());
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> saveComposition());
        view.getProductSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                view.setLoadLatestVersionVisible(newValue != null && !isNewProductOption(newValue))
        );
        view.getLoadLatestVersionButton().setOnAction(e -> loadLatestVersion());
        view.setLoadLatestVersionVisible(false);

    }

    private void loadLatestVersion() {
        Product selectedProduct = view.getProductSelector().getValue();

        if (selectedProduct == null || isNewProductOption(selectedProduct)) {
            return;
        }

        Optional<Composition> latestComposition = compositionRepo.findLatestByProduct(selectedProduct.id());
        if (latestComposition.isEmpty()) {
            showWarning("Nessuna versione trovata", "Non esiste ancora una composizione da caricare per questo prodotto.");
            return;
        }

        Composition composition = latestComposition.get();
        List<LayerDraft> layerDrafts = new ArrayList<>();
        Map<Integer, LayerDraft> byLayer = new TreeMap<>();

        for (CompositionLayerIngredient ingredient : compositionLayerIngredientRepo.findByCompositionId(composition.id())) {
            LayerDraft draft = byLayer.computeIfAbsent(ingredient.layerNumber(), LayerDraft::new);
            draft.ingredients().add(new IngredientDraft(
                    ingredient.powderId(),
                    ingredient.percentage()
            ));
        }

        layerDrafts.addAll(byLayer.values());
        view.setNotes(composition.notes());
        view.replaceLayers(layerDrafts);
    }

    private void saveComposition() {
        Product product = view.getProductSelector().getValue();

        if (product == null || isNewProductOption(product)) {
            Optional<Product> maybeNewProduct = showMissingProductDialog();
            if (maybeNewProduct.isEmpty()) {
                return;
            }
            product = maybeNewProduct.get();
        }

        BlankModel blankModel = view.getBlankModelSelector().getValue();
        if (blankModel == null) {
            showWarning("Modello blank mancante", "Seleziona un modello blank prima di salvare la composizione.");
            return;
        }

        view.renumberLayers();
        int numLayers = view.getLayers().size();
        if (numLayers == 0) {
            showWarning("Layer mancanti", "Aggiungi almeno uno strato alla composizione.");
            return;
        }
        if (numLayers != blankModel.numLayers()) {
            showWarning(
                    "Numero layer non coerente",
                    "La composizione ha " + numLayers + " layer, ma il modello blank selezionato richiede "
                            + blankModel.numLayers() + " layer."
            );
            return;
        }

        int newVersion = compositionRepo
                .findMaxVersionByProduct(product.id())
                .map(v -> v + 1)
                .orElse(1);

        Composition composition = new Composition(
                0,
                product.id(),
                newVersion,
                numLayers,
                LocalDateTime.now(),
                view.getNotes()
        );

        try {
            int compositionId = compositionRepo.insert(composition);

            for (LayerDraft layerDraft : view.getLayers()) {
                for (IngredientDraft ing : layerDraft.ingredients()) {
                    compositionLayerIngredientRepo.insert(new CompositionLayerIngredient(
                            compositionId,
                            layerDraft.layerNumber(),
                            ing.powderId(),
                            ing.percentage()
                    ));
                }
            }

            // Attiva la composizione (trigger valida layer al 100%)
            compositionRepo.setActiveComposition(product.id(), compositionId);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Composizione Salvata");
            alert.setContentText("La ricetta è stata registrata correttamente.");
            alert.showAndWait();

            app.showLaboratory();
        } catch (RuntimeException ex) {
            showDbError("Errore salvataggio composizione", ex);
        }
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

        String newProductCode = result.get().trim();
        if (newProductCode.isEmpty()) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setHeaderText("Codice prodotto mancante");
            warning.setContentText("Inserisci un codice prodotto valido per continuare.");
            warning.showAndWait();
            return Optional.empty();
        }

        try {
            Product newProduct = productRepo.insert(newProductCode, null);
            view.getProductSelector().getItems().add(newProduct);
            view.getProductSelector().getItems().remove(NEW_PRODUCT_OPTION);
            view.getProductSelector().getItems().add(0, NEW_PRODUCT_OPTION);
            view.getProductSelector().setValue(newProduct);
            return Optional.of(newProduct);
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
