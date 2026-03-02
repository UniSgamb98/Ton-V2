package com.orodent.tonv2.features.laboratory.view.partial;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class HeightRangeRowView extends HBox {

    private final TextField minHeightField = new TextField();
    private final TextField maxHeightField = new TextField();
    private final TextField superiorField = new TextField();
    private final TextField inferiorField = new TextField();
    private final Button removeButton = new Button("Rimuovi");

    public HeightRangeRowView(HeightRangeDraft preset) {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);

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

        getChildren().addAll(
                new Label("Da"), minHeightField,
                new Label("a"), maxHeightField,
                new Label("Sup"), superiorField,
                new Label("Inf"), inferiorField,
                removeButton
        );
    }

    public HeightRangeDraft toDraft() {
        return new HeightRangeDraft(
                minHeightField.getText(),
                maxHeightField.getText(),
                superiorField.getText(),
                inferiorField.getText()
        );
    }

    public Button getRemoveButton() {
        return removeButton;
    }
}
