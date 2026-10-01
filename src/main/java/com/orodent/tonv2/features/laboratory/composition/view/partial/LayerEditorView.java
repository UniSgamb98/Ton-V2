package com.orodent.tonv2.features.laboratory.composition.view.partial;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.composition.service.LayerMetricsService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public class LayerEditorView extends HBox {

    private final LayerDraft layerDraft;
    private final List<Powder> availablePowders;
    private final LayerMetricsService layerMetricsService = new LayerMetricsService();

    private final VBox ingredientsBox = new VBox(5);
    private Label title;
    private final Label metricsLabel = new Label();

    private final Button addIngredientBtn = new Button("Aggiungi polvere");
    private final Button removeLayerBtn = new Button("✕");

    private Runnable onRemove;
    private Runnable onMetricsChanged;

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

        metricsLabel.getStyleClass().add("layer-title");
        metricsLabel.setVisible(false);
        metricsLabel.setManaged(false);

        removeLayerBtn.setOnAction(e -> {
            if (onRemove != null) {
                onRemove.run();
            }
        });

        HBox header = new HBox(8, removeLayerBtn, title, metricsLabel);
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

        refreshMetrics();
    }

    public void setLayerNumber(int number) {
        title.setText("Layer " + number);
        refreshMetrics();
    }

    public void setOnRemove(Runnable action) {
        this.onRemove = action;
    }

    public void setLayerRemovalEnabled(boolean enabled) {
        removeLayerBtn.setVisible(enabled);
        removeLayerBtn.setManaged(enabled);
    }

    public void setOnMetricsChanged(Runnable onMetricsChanged) {
        this.onMetricsChanged = onMetricsChanged;
    }

    public int getIngredientCount() {
        return layerDraft.ingredients().size();
    }

    public double getTotalPercentage() {
        return layerDraft.ingredients().stream()
                .mapToDouble(IngredientDraft::percentage)
                .filter(value -> value > 0)
                .sum();
    }

    public String getMetricsText() {
        return metricsLabel.getText();
    }

    private void addIngredient() {
        IngredientDraft ingredient = new IngredientDraft(0, 0);
        layerDraft.ingredients().add(ingredient);
        addIngredientRow(ingredient);
        refreshMetrics();
    }

    private void addIngredientRow(IngredientDraft ingredient) {
        IngredientRowView rowView = new IngredientRowView(ingredient);
        rowView.setAvailablePowders(availablePowders);
        rowView.setPowderById(ingredient.powderId());
        rowView.setPercentage(ingredient.percentage());
        rowView.setOnIngredientChanged(this::refreshMetrics);

        rowView.setOnRemove(() -> {
            layerDraft.ingredients().remove(ingredient);
            ingredientsBox.getChildren().remove(rowView);
            refreshMetrics();
        });

        ingredientsBox.getChildren().add(rowView);
    }

    private void refreshMetrics() {
        LayerMetricsService.LayerMetrics metrics = layerMetricsService.calculate(layerDraft, availablePowders);

        String layerText = "Traslucenza: " + formatDecimal(metrics.weightedTranslucency(), 2);
        String strengthText = "Resistenza: " + formatDecimal(metrics.weightedStrength(), 0) + " MPa";
        String yttriaText = "Ittria: " + metrics.yttriaSummary();

        metricsLabel.setText(layerText + "\n" + strengthText + "\n" + yttriaText);
        if (onMetricsChanged != null) {
            onMetricsChanged.run();
        }
    }

    private String formatDecimal(Double value, int decimals) {
        if (value == null) {
            return "n/d";
        }
        return String.format(Locale.US, "%1$." + decimals + "f", value);
    }
}
