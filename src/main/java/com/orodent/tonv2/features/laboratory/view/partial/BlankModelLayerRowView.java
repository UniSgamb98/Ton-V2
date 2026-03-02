package com.orodent.tonv2.features.laboratory.view.partial;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class BlankModelLayerRowView extends HBox {

    private final Label layerLabel = new Label();
    private final TextField occupiedSpaceField = new TextField();
    private final Button removeButton = new Button("Rimuovi");

    public BlankModelLayerRowView(int layerNumber) {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);

        setLayerNumber(layerNumber);
        occupiedSpaceField.setPromptText("Es. 35");
        HBox.setHgrow(occupiedSpaceField, Priority.ALWAYS);

        getChildren().addAll(layerLabel, new Label("spazio occupato"), occupiedSpaceField, new Label("%"), removeButton);
    }

    public void setLayerNumber(int layerNumber) {
        layerLabel.setText("Strato " + layerNumber);
    }

    public BlankModelLayerDraft toDraft(int layerNumber) {
        return new BlankModelLayerDraft(layerNumber, occupiedSpaceField.getText());
    }

    public Button getRemoveButton() {
        return removeButton;
    }
}
