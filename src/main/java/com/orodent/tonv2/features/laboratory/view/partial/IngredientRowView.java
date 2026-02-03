package com.orodent.tonv2.features.laboratory.view.partial;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class IngredientRowView extends HBox {

    private final IngredientDraft ingredient;

    private final ComboBox<Powder> powderSelector = new ComboBox<>();
    private final TextField percentageField = new TextField();
    private final Button removeBtn = new Button("✕");

    public IngredientRowView(IngredientDraft ingredient) {
        this.ingredient = ingredient;
        buildUI();
    }

    private void buildUI() {

        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("ingredient-row");

        /* ---- Powder selector ---- */

        powderSelector.setPromptText("Polvere");
        powderSelector.setPrefWidth(200);

        // verrà popolato dal controller (come per Product)
        powderSelector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                ingredient.setPowderId(val.id());
            }
        });

        /* ---- Percentage ---- */

        percentageField.setPromptText("%");
        percentageField.setPrefWidth(60);

        percentageField.textProperty().addListener((obs, old, val) -> {
            try {
                double pct = Double.parseDouble(val.replace(',', '.'));
                ingredient.setPercentage(pct);
            } catch (NumberFormatException e) {
                ingredient.setPercentage(0);
            }
        });

        /* ---- Remove ---- */

        getChildren().addAll(
                powderSelector,
                percentageField,
                removeBtn
        );
    }

    public void setOnRemove(Runnable action) {
        removeBtn.setOnAction(e -> action.run());
    }

    /* =======================
       GETTER PER IL CONTROLLER
       ======================= */

    public ComboBox<Powder> getPowderSelector() {
        return powderSelector;
    }
}
