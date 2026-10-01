package com.orodent.tonv2.core.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class EditorActionBar extends HBox {

    public EditorActionBar(String styleClass, Node leadingAction, Node status, Node primaryAction) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add(styleClass);

        if (leadingAction != null) {
            getChildren().add(leadingAction);
        }
        if (status != null) {
            getChildren().add(status);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        if (primaryAction != null) {
            getChildren().add(primaryAction);
        }
    }
}
