package com.orodent.tonv2.features.laboratory.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelView extends VBox {

    private final AppHeader header = new AppHeader("Nuovo modello disco");
    private final BorderPane content = new BorderPane();

    private final TextField codeField = new TextField();
    private final TextField diameterField = new TextField();
    private final TextField superiorOvermaterialField = new TextField();
    private final TextField inferiorOvermaterialField = new TextField();
    private final TextField pressureField = new TextField();
    private final TextField gramsPerMmField = new TextField();

    private final VBox rangesBox = new VBox(8);
    private final Button addRangeBtn = new Button("Aggiungi fascia altezza");
    private final Button saveBtn = new Button("Salva modello disco");

    private final List<HeightRangeRow> rangeRows = new ArrayList<>();

    public CreateDiskModelView() {
        setSpacing(20);
        setPadding(new Insets(20));
        buildLayout();
        getChildren().addAll(header, content);
    }

    private void buildLayout() {
        GridPane baseForm = new GridPane();
        baseForm.setHgap(12);
        baseForm.setVgap(10);

        codeField.setPromptText("Es. BM-98-A");
        diameterField.setPromptText("Es. 98.0");
        superiorOvermaterialField.setPromptText("Es. 1.2");
        inferiorOvermaterialField.setPromptText("Es. 0.7");
        pressureField.setPromptText("Es. 2300");
        gramsPerMmField.setPromptText("Es. 0.550");

        baseForm.add(new Label("Codice"), 0, 0);
        baseForm.add(codeField, 1, 0);
        baseForm.add(new Label("Diametro (mm)"), 2, 0);
        baseForm.add(diameterField, 3, 0);

        baseForm.add(new Label("Overmaterial superiore default (mm)"), 0, 1);
        baseForm.add(superiorOvermaterialField, 1, 1);
        baseForm.add(new Label("Overmaterial inferiore default (mm)"), 2, 1);
        baseForm.add(inferiorOvermaterialField, 3, 1);

        baseForm.add(new Label("Pressione (kg/cm²)"), 0, 2);
        baseForm.add(pressureField, 1, 2);
        baseForm.add(new Label("Grammi per mm"), 2, 2);
        baseForm.add(gramsPerMmField, 3, 2);

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);

        baseForm.getColumnConstraints().addAll(labelCol, fieldCol, labelCol, fieldCol);
        codeField.setMaxWidth(Double.MAX_VALUE);
        diameterField.setMaxWidth(Double.MAX_VALUE);
        superiorOvermaterialField.setMaxWidth(Double.MAX_VALUE);
        inferiorOvermaterialField.setMaxWidth(Double.MAX_VALUE);
        pressureField.setMaxWidth(Double.MAX_VALUE);
        gramsPerMmField.setMaxWidth(Double.MAX_VALUE);

        Label rangesLabel = new Label("Overmaterial per fascia altezza (opzionale)");
        addRangeBtn.setOnAction(e -> addRangeRow(null));

        VBox centerBox = new VBox(12, baseForm, rangesLabel, rangesBox, addRangeBtn);
        centerBox.setPadding(new Insets(10));
        centerBox.setMaxWidth(950);

        StackPane centered = new StackPane(centerBox);
        centered.setPadding(new Insets(0, 10, 0, 10));
        StackPane.setAlignment(centerBox, Pos.TOP_CENTER);

        HBox bottom = new HBox(saveBtn);
        bottom.setPadding(new Insets(10));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        content.setCenter(centered);
        content.setBottom(bottom);
    }

    private void addRangeRow(HeightRangeDraft preset) {
        HeightRangeRow row = new HeightRangeRow(preset);
        rangeRows.add(row);

        row.removeButton.setOnAction(e -> {
            rangeRows.remove(row);
            rangesBox.getChildren().remove(row.container);
        });

        rangesBox.getChildren().add(row.container);
    }

    public AppHeader getHeader() {
        return header;
    }

    public Button getSaveButton() {
        return saveBtn;
    }

    public String getCode() { return codeField.getText(); }
    public String getDiameter() { return diameterField.getText(); }
    public String getSuperiorOvermaterial() { return superiorOvermaterialField.getText(); }
    public String getInferiorOvermaterial() { return inferiorOvermaterialField.getText(); }
    public String getPressure() { return pressureField.getText(); }
    public String getGramsPerMm() { return gramsPerMmField.getText(); }

    public List<HeightRangeDraft> getRangeDrafts() {
        List<HeightRangeDraft> drafts = new ArrayList<>();
        for (HeightRangeRow row : rangeRows) {
            drafts.add(new HeightRangeDraft(
                    row.minHeightField.getText(),
                    row.maxHeightField.getText(),
                    row.superiorField.getText(),
                    row.inferiorField.getText()
            ));
        }
        return drafts;
    }

    public record HeightRangeDraft(String minHeight, String maxHeight, String superiorOvermaterial, String inferiorOvermaterial) {}

    private static class HeightRangeRow {
        private final HBox container;
        private final TextField minHeightField = new TextField();
        private final TextField maxHeightField = new TextField();
        private final TextField superiorField = new TextField();
        private final TextField inferiorField = new TextField();
        private final Button removeButton = new Button("Rimuovi");

        private HeightRangeRow(HeightRangeDraft preset) {
            minHeightField.setPromptText("Min mm");
            maxHeightField.setPromptText("Max mm");
            superiorField.setPromptText("Over sup.");
            inferiorField.setPromptText("Over inf.");

            if (preset != null) {
                minHeightField.setText(preset.minHeight());
                maxHeightField.setText(preset.maxHeight());
                superiorField.setText(preset.superiorOvermaterial());
                inferiorField.setText(preset.inferiorOvermaterial());
            }

            HBox.setHgrow(minHeightField, Priority.ALWAYS);
            HBox.setHgrow(maxHeightField, Priority.ALWAYS);
            HBox.setHgrow(superiorField, Priority.ALWAYS);
            HBox.setHgrow(inferiorField, Priority.ALWAYS);

            container = new HBox(8,
                    new Label("Da"), minHeightField,
                    new Label("a"), maxHeightField,
                    new Label("Sup"), superiorField,
                    new Label("Inf"), inferiorField,
                    removeButton
            );
            container.setAlignment(Pos.CENTER_LEFT);
        }
    }
}
