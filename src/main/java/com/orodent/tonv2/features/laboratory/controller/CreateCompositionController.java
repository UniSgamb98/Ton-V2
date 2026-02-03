package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;

import com.orodent.tonv2.core.database.model.*;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.view.CreateCompositionView;
import javafx.scene.control.*;

import java.time.LocalDateTime;

public class CreateCompositionController {

    private final CreateCompositionView view;
    private final AppController app;
    private final PowderRepository powderRepo;
    private final CompositionRepository compositionRepo;
    private final CompositionLayerRepository compositionLayerRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final ProductRepository productRepo;

    public CreateCompositionController(CreateCompositionView view, AppController app, PowderRepository powderRepo, CompositionRepository compositionRepo, CompositionLayerRepository compositionLayerRepo, CompositionLayerIngredientRepository compositionLayerIngredientRepo, ProductRepository productRepo) {
        this.view = view;
        this.app = app;
        this.powderRepo = powderRepo;
        this.compositionRepo = compositionRepo;
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
    }
    private void loadPowders() {
        view.setAvailablePowders(powderRepo.findAll());
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> saveComposition());
       // view.getCancelButton().setOnAction(e -> app.showLaboratory());

    }

    /** Quando si salva la composizione */
    private void saveComposition() {

        // 1️⃣ Leggo il Product dalla view
        Product product = view.getProductSelector().getValue();

        if (product == null) {
            System.out.println("Seleziona un prodotto prima di salvare la composizione");
            //TODO showError("Seleziona un prodotto prima di salvare la composizione");
            return;
        }

        // 2️⃣ Calcolo nuova versione per quel product
        int newVersion = compositionRepo
                .findMaxVersionByProduct(product.id())
                .map(v -> v + 1)
                .orElse(1);

        // 3️⃣ Disattivo eventuale composizione attiva precedente
        compositionRepo.deactivateActiveByProduct(product.id());

        // 4️⃣ Creo e salvo la composizione
        Composition composition = new Composition(
                0,
                product.id(),
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
}
