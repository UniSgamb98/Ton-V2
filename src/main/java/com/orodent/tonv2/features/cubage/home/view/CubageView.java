package com.orodent.tonv2.features.cubage.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CubageView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label introLabel = new Label("Caricamento...");
    private final Button creationButton = new Button("Gestione Calcoli Cubaggio");
    private final Button productAssignmentButton = new Button("Assegna Formula Set ai Product");
    private final Button payloadContractButton = new Button("Payload Contract");

    public CubageView() {
        setSpacing(16);
        setPadding(new Insets(20));

        introLabel.setWrapText(true);
        HBox actions = new HBox(12, creationButton, productAssignmentButton, payloadContractButton);
        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(header, introLabel, separator, actions);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setIntroText(String text) {
        introLabel.setText(text == null ? "" : text);
    }

    public Button getCreationButton() {
        return creationButton;
    }

    public Button getProductAssignmentButton() {
        return productAssignmentButton;
    }

    public Button getPayloadContractButton() {
        return payloadContractButton;
    }
}
