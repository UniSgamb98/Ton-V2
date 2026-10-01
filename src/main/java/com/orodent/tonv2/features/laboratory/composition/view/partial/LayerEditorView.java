package com.orodent.tonv2.features.laboratory.composition.view.partial;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class LayerEditorView extends HBox {

    private final LayerDraft layerDraft;
    private final List<Powder> availablePowders;

    private final VBox ingredientsBox = new VBox(5);
    private Label title;

    private final Button addIngredientBtn = new Button("Aggiungi polvere");
    private final Button removeLayerBtn = new Button("✕");

    private Runnable onRemove;
    private Runnable onChanged;

    public LayerEditorView(LayerDraft layerDraft, List<Powder> availablePowders) {
        this.layerDraft = layerDraft;
        this.availablePowders = availablePowders;
        buildUI();
    }

    private void buildUI() {
        setSpacing(10);
        getStyleClass().add("layer-editor");
        setMaxWidth(Double.MAX_VALUE);

        title = new Label("Layer " + layerDraft.layerNumber());
        title.getStyleClass().add("layer-title");

        removeLayerBtn.setOnAction(e -> {
            if (onRemove != null) {
                onRemove.run();
            }
        });

        HBox header = new HBox(8, removeLayerBtn, title);
        header.setAlignment(Pos.CENTER_LEFT);

        ingredientsBox.setSpacing(5);
        for (IngredientDraft ingredient : layerDraft.ingredients()) {
            addIngredientRow(ingredient);
        }

        addIngredientBtn.setOnAction(e -> addIngredient());
        addIngredientBtn.getStyleClass().add("add-ingredient-action");

        VBox body = new VBox(10, header, ingredientsBox, addIngredientBtn);
        body.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(body, javafx.scene.layout.Priority.ALWAYS);
        getChildren().add(body);

        notifyChanged();
    }

    public void setLayerNumber(int number) {
        title.setText("Layer " + number);
        notifyChanged();
    }

    public void setOnRemove(Runnable action) {
        this.onRemove = action;
    }

    public void setLayerRemovalEnabled(boolean enabled) {
        removeLayerBtn.setVisible(enabled);
        removeLayerBtn.setManaged(enabled);
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void addIngredient() {
        IngredientDraft ingredient = new IngredientDraft(0, 0);
        layerDraft.ingredients().add(ingredient);
        addIngredientRow(ingredient);
        notifyChanged();
    }

    private void addIngredientRow(IngredientDraft ingredient) {
        IngredientRowView rowView = new IngredientRowView(ingredient);
        rowView.setAvailablePowders(availablePowders);
        rowView.setPowderById(ingredient.powderId());
        rowView.setPercentage(ingredient.percentage());
        rowView.setOnIngredientChanged(this::notifyChanged);

        rowView.setOnRemove(() -> {
            layerDraft.ingredients().remove(ingredient);
            ingredientsBox.getChildren().remove(rowView);
            notifyChanged();
        });

        ingredientsBox.getChildren().add(rowView);
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
