package com.orodent.tonv2.features.laboratory.controller;

import com.orodent.tonv2.app.AppController;

import com.orodent.tonv2.core.database.model.*;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.view.CreateCompositionView;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.time.LocalDateTime;

public class CreateCompositionController {

    private final CreateCompositionView view;
    private final AppController app;
    private int layerCounter = 1;
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
        setupActions();
    }

    /** Carica gli articoli dal repo */
    private void loadProducts() {
        view.getProductSelector().getItems().setAll(productRepo.findAll());
    }

    private void setupActions() {

        view.getAddLayerBtn().setOnAction(e -> addNewLayer());

        view.getSaveBtn().setOnAction(e -> saveComposition());

        view.getCancelBtn().setOnAction(e -> app.showLaboratory()); // torna al menu laboratorio
    }

    /** Aggiunge un nuovo layer */
    private void addNewLayer() {

        VBox layerBox = new VBox(10);
        layerBox.setPadding(new Insets(10));
        layerBox.getStyleClass().add("layer-box");

        Label layerTitle = new Label("Strato " + layerCounter++);

        VBox ingredientList = new VBox(5);

        Button addIngredientBtn = new Button("Aggiungi Polvere");
        addIngredientBtn.setOnAction(e -> ingredientList.getChildren().add(createIngredientRow()));

        layerBox.getChildren().addAll(layerTitle, ingredientList, addIngredientBtn);

        view.addLayer(layerBox);
    }

    /** Riga di un ingrediente: polvere + percentuale */
    private HBox createIngredientRow() {
        ComboBox<String> powderSelector = new ComboBox<>();
        TextField percentageField = new TextField();

        powderSelector.setPromptText("Polvere");
        percentageField.setPromptText("%");

        powderSelector.getItems().addAll(
                powderRepo.findAll().stream().map(Powder::name).toList()
        );


        HBox row = new HBox(10, powderSelector, percentageField);
        row.setPadding(new Insets(5));
        return row;
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
