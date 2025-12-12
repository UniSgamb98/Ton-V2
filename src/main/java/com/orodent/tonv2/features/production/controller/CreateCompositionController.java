package com.orodent.tonv2.features.production.controller;

import com.orodent.tonv2.app.AppController;

import com.orodent.tonv2.features.production.model.Powder;
import com.orodent.tonv2.features.production.repository.PowderRepository;
import com.orodent.tonv2.features.production.view.CreateCompositionView;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class CreateCompositionController {

    private final CreateCompositionView view;
    private final AppController app;

    private int layerCounter = 1;
    private final PowderRepository powderRepo;

    public CreateCompositionController(CreateCompositionView view, AppController app, PowderRepository powderRepo) {
        this.view = view;
        this.app = app;
        this.powderRepo = powderRepo;

        loadItems();
        setupActions();
    }

    /** Carica gli articoli dal repo */
    private void loadItems() {
        // TODO: qui inserire itemRepo.findAll()
        view.getItemSelector().getItems().addAll("Articolo A", "Articolo B", "Articolo C");

        view.getItemSelector().setOnAction(e -> loadLotsForItem());
    }

    /** Carica i lotti del prodotto selezionato */
    private void loadLotsForItem() {
        view.getLotSelector().getItems().clear();

        // TODO: sostituire con lotRepo.findByItem(...)
        view.getLotSelector().getItems().addAll("Lot 123", "Lot 456", "Lot 789");
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

        // TODO: costruire DTO
        // TODO: salvare usando compositionService

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Composizione Salvata");
        alert.setContentText("La ricetta è stata registrata correttamente.");
        alert.showAndWait();

        app.showLaboratory();
    }
}
