package com.orodent.tonv2.features.laboratory.view.partial;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class LayerEditorView extends HBox {

    private final LayerDraft layerDraft;
    private final List<Powder> availablePowders;

    private final VBox ingredientsBox = new VBox(5);
    private final TextArea notesArea = new TextArea();
    private Label title;

    private final Button addIngredientBtn = new Button("Aggiungi polvere");
    private final Button removeLayerBtn = new Button("✕");

    public LayerEditorView(LayerDraft layerDraft, List<Powder> availablePowders) {
        this.layerDraft = layerDraft;
        this.availablePowders = availablePowders;
        buildUI();
    }

    private void buildUI() {

        setSpacing(10);
        setPadding(new Insets(10));
        getStyleClass().add("layer-editor");
        getStyleClass().add("light-pane");

        title = new Label("Layer " + layerDraft.layerNumber());
        title.getStyleClass().add("layer-title");
        HBox header = new HBox(10, title, removeLayerBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        /* ---- Note layer ---- */

        notesArea.setPromptText("Note layer...");
        notesArea.setWrapText(true);
        notesArea.setText(layerDraft.notes());
        updateNotesAreaSize();


        notesArea.textProperty().addListener((obs, old, val) ->
                layerDraft.setNotes(val)
        );

        /* ---- Ingredient list ---- */

        ingredientsBox.setSpacing(5);
        for (IngredientDraft ingredient : layerDraft.ingredients()) {
            addIngredientRow(ingredient);
        }

        /* ---- Add ingredient ---- */

        addIngredientBtn.setOnAction(e -> addIngredient());

        VBox leftBody = new VBox(10, header, ingredientsBox, addIngredientBtn);
        VBox rightBody = new VBox(new Label("Note"), notesArea);
        HBox.setHgrow(rightBody, Priority.ALWAYS);
        getChildren().addAll(leftBody, rightBody);
    }

    public void setLayerNumber(int number) {
        title.setText("Layer " + number);
    }

    private void addIngredient() {
        IngredientDraft ingredient = new IngredientDraft(0, 0);
        layerDraft.ingredients().add(ingredient);
        addIngredientRow(ingredient);
    }

    private void addIngredientRow(IngredientDraft ingredient) {
        IngredientRowView rowView = new IngredientRowView(ingredient);
        rowView.setAvailablePowders(availablePowders);
        rowView.setPowderById(ingredient.powderId());
        rowView.setPercentage(ingredient.percentage());

        rowView.setOnRemove(() -> {
            layerDraft.ingredients().remove(ingredient);
            ingredientsBox.getChildren().remove(rowView);
            updateNotesAreaSize();
        });

        ingredientsBox.getChildren().add(rowView);
        updateNotesAreaSize();
    }

    private void updateNotesAreaSize() {
        int ingredientCount = layerDraft.ingredients().size();
        int preferredRows = ingredientCount * (5/2) + 2;
        notesArea.setPrefRowCount(preferredRows);
    }

    public void setOnRemove(Runnable action) {
        removeLayerBtn.setOnAction(e -> action.run());
    }
}
