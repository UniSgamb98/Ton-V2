package com.orodent.tonv2.features.laboratory.view.partial;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.service.LayerMetricsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public class LayerEditorView extends HBox {

    private final LayerDraft layerDraft;
    private final List<Powder> availablePowders;
    private final LayerMetricsService layerMetricsService = new LayerMetricsService();

    private final VBox ingredientsBox = new VBox(5);
    private final TextArea notesArea = new TextArea();
    private Label title;
    private final Label metricsLabel = new Label();

    private final Button addIngredientBtn = new Button("Aggiungi polvere");
    private final Button removeLayerBtn = new Button("✕");

    private Runnable onRemove;
    private Runnable onLayerChanged;

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

        metricsLabel.getStyleClass().add("layer-title");

        removeLayerBtn.setOnAction(e -> {
            if (onRemove != null) {
                onRemove.run();
            }
        });

        HBox header = new HBox(8, removeLayerBtn, title, metricsLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        notesArea.setPromptText("Note layer...");
        notesArea.setWrapText(true);
        notesArea.setText(layerDraft.notes());
        updateNotesAreaSize();

        notesArea.textProperty().addListener((obs, old, val) -> {
            layerDraft.setNotes(val);
            notifyLayerChanged();
        });

        ingredientsBox.setSpacing(5);
        for (IngredientDraft ingredient : layerDraft.ingredients()) {
            addIngredientRow(ingredient);
        }

        addIngredientBtn.setOnAction(e -> addIngredient());

        VBox leftBody = new VBox(10, header, ingredientsBox, addIngredientBtn);
        VBox rightBody = new VBox(new Label("Note"), notesArea);
        HBox.setHgrow(rightBody, Priority.ALWAYS);
        getChildren().addAll(leftBody, rightBody);

        refreshMetrics();
    }

    public void setLayerNumber(int number) {
        title.setText("Layer " + number);
        refreshMetrics();
    }

    public void setOnRemove(Runnable action) {
        this.onRemove = action;
    }

    public void setOnLayerChanged(Runnable onLayerChanged) {
        this.onLayerChanged = onLayerChanged;
    }

    private void addIngredient() {
        IngredientDraft ingredient = new IngredientDraft(0, 0);
        layerDraft.ingredients().add(ingredient);
        addIngredientRow(ingredient);
        notifyLayerChanged();
    }

    private void addIngredientRow(IngredientDraft ingredient) {
        IngredientRowView rowView = new IngredientRowView(ingredient);
        rowView.setAvailablePowders(availablePowders);
        rowView.setPowderById(ingredient.powderId());
        rowView.setPercentage(ingredient.percentage());
        rowView.setOnIngredientChanged(this::notifyLayerChanged);

        rowView.setOnRemove(() -> {
            layerDraft.ingredients().remove(ingredient);
            ingredientsBox.getChildren().remove(rowView);
            updateNotesAreaSize();
            notifyLayerChanged();
        });

        ingredientsBox.getChildren().add(rowView);
        updateNotesAreaSize();
    }

    private void updateNotesAreaSize() {
        int ingredientCount = layerDraft.ingredients().size();
        int preferredRows = ingredientCount * (5 / 2) + 2;
        notesArea.setPrefRowCount(preferredRows);
    }

    private void notifyLayerChanged() {
        refreshMetrics();
        if (onLayerChanged != null) {
            onLayerChanged.run();
        }
    }

    private void refreshMetrics() {
        LayerMetricsService.LayerMetrics metrics = layerMetricsService.calculate(layerDraft, availablePowders);

        String layerText = "| T: " + formatDecimal(metrics.weightedTranslucency(), 2);
        String strengthText = "| R: " + formatDecimal(metrics.weightedStrength(), 0) + " MPa";
        String yttriaText = "| " + metrics.yttriaSummary();

        metricsLabel.setText(layerText + " " + strengthText + " " + yttriaText);
    }

    private String formatDecimal(Double value, int decimals) {
        if (value == null) {
            return "n/d";
        }
        return String.format(Locale.US, "%1$." + decimals + "f", value);
    }
}
