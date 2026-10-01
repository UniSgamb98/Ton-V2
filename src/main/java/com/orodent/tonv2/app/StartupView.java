package com.orodent.tonv2.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class StartupView extends VBox {

    private final Label titleLabel = new Label("TON");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label("Avvio database...");
    private final Button retryButton = new Button("Riprova");
    private final Button closeButton = new Button("Chiudi applicazione");

    public StartupView() {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(32));

        titleLabel.getStyleClass().add("page-title");
        progressIndicator.setMaxSize(52, 52);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(480);
        statusLabel.setAlignment(Pos.CENTER);
        retryButton.setVisible(false);
        retryButton.setManaged(false);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        getChildren().addAll(titleLabel, progressIndicator, statusLabel, retryButton, closeButton);
    }

    public Button getRetryButton() {
        return retryButton;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void showLoading() {
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusLabel.setText("Avvio database...");
        retryButton.setVisible(false);
        retryButton.setManaged(false);
        closeButton.setVisible(false);
        closeButton.setManaged(false);
    }

    public void showError(String message) {
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setText(message == null || message.isBlank()
                ? "Impossibile avviare il database."
                : message);
        retryButton.setVisible(true);
        retryButton.setManaged(true);
        closeButton.setVisible(true);
        closeButton.setManaged(true);
    }
}
