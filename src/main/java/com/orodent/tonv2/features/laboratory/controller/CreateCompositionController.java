package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;

import com.orodent.tonv2.core.database.model.*;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.view.CreateCompositionView;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreateCompositionController {

    private static final Product NEW_PRODUCT_OPTION = new Product(-1, "➕ Nuovo prodotto", "");

    private final CreateCompositionView view;
    private final AppController app;
    private final PowderRepository powderRepo;
    private final CompositionRepository compositionRepo;
    private final ItemRepository itemRepo;
    private final CompositionLayerRepository compositionLayerRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final ProductRepository productRepo;

    public CreateCompositionController(CreateCompositionView view, AppController app, PowderRepository powderRepo, CompositionRepository compositionRepo, CompositionLayerRepository compositionLayerRepo, CompositionLayerIngredientRepository compositionLayerIngredientRepo, ProductRepository productRepo, ItemRepository itemRepo) {
        this.view = view;
        this.app = app;
        this.powderRepo = powderRepo;
        this.compositionRepo = compositionRepo;
        this.itemRepo = itemRepo;
        this.compositionLayerRepo = compositionLayerRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.productRepo = productRepo;

        loadProducts();
        loadPowders();
        setupActions();
    }

    /** Carica gli articoli dal repo */
    private void loadProducts() {
        view.getProductSelector().getItems().setAll(productRepo.findAll());
        if (!view.getProductSelector().getItems().contains(NEW_PRODUCT_OPTION)) {
            view.getProductSelector().getItems().add(0, NEW_PRODUCT_OPTION);
        }
    }
    private void loadPowders() {
        view.setAvailablePowders(powderRepo.findAll());
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

        Item item = findExistingItemForProduct(selectedProduct);
        if (item == null) {
            showWarning("Nessuna versione trovata", "Il prodotto selezionato non ha ancora versioni salvate.");
            return;
        }

        Optional<Composition> latestComposition = compositionRepo.findLatestByProduct(item.id());
        if (latestComposition.isEmpty()) {
            showWarning("Nessuna versione trovata", "Non esiste ancora una composizione da caricare per questo prodotto.");
            return;
        }

        Composition composition = latestComposition.get();
        List<LayerDraft> layerDrafts = new ArrayList<>();

        for (CompositionLayer layer : compositionLayerRepo.findByCompositionId(composition.id())) {
            LayerDraft draft = new LayerDraft(layer.layerNumber());
            draft.setNotes(layer.notes());

            for (CompositionLayerIngredient ingredient : compositionLayerIngredientRepo.findByLayerId(layer.id())) {
                draft.ingredients().add(new IngredientDraft(
                        ingredient.powderId(),
                        ingredient.percentage()
                ));
            }

            layerDrafts.add(draft);
        }

        view.setNotes(composition.notes());
        view.replaceLayers(layerDrafts);
    }

    /** Quando si salva la composizione */
    private void saveComposition() {

        // 1️⃣ Leggo il Product dalla view
        Product product = view.getProductSelector().getValue();

        if (product == null || isNewProductOption(product)) {
            Optional<Product> maybeNewProduct = showMissingProductDialog();
            if (maybeNewProduct.isEmpty()) {
                return;
            }
            product = maybeNewProduct.get();
        }

        Item item = resolveItemForProduct(product);

        // 2️⃣ Calcolo nuova versione per quell'item
        int newVersion = compositionRepo
                .findMaxVersionByProduct(item.id())
                .map(v -> v + 1)
                .orElse(1);

        // 3️⃣ Disattivo eventuale composizione attiva precedente
        compositionRepo.deactivateActiveByProduct(item.id());

        // 4️⃣ Creo e salvo la composizione
        Composition composition = new Composition(
                0,
                item.id(),
                newVersion,
                true,
                LocalDateTime.now(),
                view.getNotes()
        );

        int compositionId = compositionRepo.insert(composition);

        // 5️⃣ Salvo layer e ingredienti
        view.renumberLayers();
        for (LayerDraft layerDraft : view.getLayers()) {

            CompositionLayer layer = new CompositionLayer(
                    0,
                    compositionId,
                    layerDraft.layerNumber(),
                    layerDraft.notes()
            );

            int layerId = compositionLayerRepo.insert(layer);

            for (IngredientDraft ing : layerDraft.ingredients()) {

                CompositionLayerIngredient cli =
                        new CompositionLayerIngredient(
                                0,
                                layerId,
                                ing.powderId(),
                                ing.percentage()
                        );

                compositionLayerIngredientRepo.insert(cli);
            }
        }

        // 6️⃣ Feedback utente
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Composizione Salvata");
        alert.setContentText("La ricetta è stata registrata correttamente.");
        alert.showAndWait();

        app.showLaboratory();
    }



    private Item resolveItemForProduct(Product product) {
        Item existing = itemRepo.findById(product.id());
        if (existing != null) {
            return existing;
        }

        String generatedCode = "PROD-" + product.id();
        Item byCode = itemRepo.findByCode(generatedCode);
        if (byCode != null) {
            return byCode;
        }

        return itemRepo.insert(generatedCode);
    }

    private Item findExistingItemForProduct(Product product) {
        Item existing = itemRepo.findById(product.id());
        if (existing != null) {
            return existing;
        }

        String generatedCode = "PROD-" + product.id();
        return itemRepo.findByCode(generatedCode);
    }

    private boolean isNewProductOption(Product product) {
        return product != null && product.id() == NEW_PRODUCT_OPTION.id();
    }

    private Optional<Product> showMissingProductDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuovo prodotto");
        dialog.setHeaderText("Product selector vuoto");
        dialog.setContentText("Nome nuovo prodotto:");

        dialog.getEditor().setPromptText("Inserisci nome prodotto");

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

        String newProductName = result.get().trim();
        if (newProductName.isEmpty()) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setHeaderText("Nome prodotto mancante");
            warning.setContentText("Inserisci un nome prodotto valido per continuare.");
            warning.showAndWait();
            return Optional.empty();
        }

        try {
            Product newProduct = productRepo.insert(newProductName);
            view.getProductSelector().getItems().add(newProduct);
            view.getProductSelector().getItems().remove(NEW_PRODUCT_OPTION);
            view.getProductSelector().getItems().add(0, NEW_PRODUCT_OPTION);
            view.getProductSelector().setValue(newProduct);
            return Optional.of(newProduct);
        } catch (RuntimeException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setHeaderText("Errore creazione prodotto");
            error.setContentText("Non è stato possibile creare il nuovo prodotto: " + e.getMessage());
            error.showAndWait();
            return Optional.empty();
        }
    }

    private void showWarning(String header, String content) {
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setHeaderText(header);
        warning.setContentText(content);
        warning.showAndWait();
    }
}
