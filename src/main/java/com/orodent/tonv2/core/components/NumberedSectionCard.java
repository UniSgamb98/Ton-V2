package com.orodent.tonv2.core.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NumberedSectionCard extends VBox {

    public NumberedSectionCard(
            String stylePrefix,
            String number,
            String titleText,
            String descriptionText,
            Node... content
    ) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add(stylePrefix + "-section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add(stylePrefix + "-section-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add(stylePrefix + "-section-description");
        description.setWrapText(true);

        HBox heading = new HBox(12, numberLabel, new VBox(2, title, description));
        heading.setAlignment(Pos.CENTER_LEFT);

        setSpacing(16);
        getChildren().add(heading);
        getChildren().addAll(content);
        getStyleClass().add(stylePrefix + "-section-card");
    }
}
