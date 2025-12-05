package com.orodent.tonv2.core.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class HoverPopup {

    private final Stage stage;
    private final StackPane container;

    public HoverPopup() {

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        container = new StackPane();
        container.getStyleClass().add("rounded-popup");
        container.setPadding(new Insets(12));

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(40);
        clip.arcHeightProperty().set(40);
        clip.widthProperty().bind(container.widthProperty());
        clip.heightProperty().bind(container.heightProperty());
        container.setClip(clip);

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);

        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm()
        );

        stage.setScene(scene);
    }

    public void setContent(Node node) {
        container.getChildren().setAll(node);
    }

    /** Mostra il popup (sia hover che click destro) */
    public void show(Node ownerNode, double screenX, double screenY) {
        Platform.runLater(() -> {
            try {
                stage.setX(screenX + 8);
                stage.setY(screenY + 8);
                stage.show();
            } catch (Exception ignored) {}
        });
    }

    /** Nasconde sempre il popup */
    public void hide() {
        Platform.runLater(stage::hide);
    }
}
