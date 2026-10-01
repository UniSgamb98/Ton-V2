package com.orodent.tonv2.features.cubage.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.components.DashboardActionCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class CubageView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1180;

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label introLabel = new Label("Caricamento...");
    private final Button creationButton = new DashboardActionCard(
            "1", "cubage-formula", "Gestione calcoli", "Configura formule, variabili e set di calcolo"
    );
    private final Button productAssignmentButton = new DashboardActionCard(
            "2", "cubage-assignment", "Assegna formule ai prodotti", "Collega i set di calcolo ai prodotti"
    );
    private final Button payloadContractButton = new DashboardActionCard(
            "3", "cubage-contract", "Payload contract", "Definisci i dati disponibili per i calcoli"
    );

    public CubageView() {
        introLabel.setWrapText(true);
        introLabel.getStyleClass().add("feature-dashboard-subtitle");

        TilePane cards = new TilePane(16, 16);
        cards.setPrefColumns(3);
        cards.setPrefTileWidth(252);
        cards.setPrefTileHeight(210);
        cards.setTileAlignment(Pos.CENTER_LEFT);
        cards.getStyleClass().add("feature-dashboard-card-grid");
        cards.getChildren().addAll(creationButton, productAssignmentButton, payloadContractButton);

        VBox content = new VBox(30,
                createIntroduction(),
                createSection(
                        "1",
                        "Configurazione cubaggio",
                        "Prepara i calcoli e collegali ai dati utilizzati dall'applicazione.",
                        cards
                )
        );
        content.setMaxWidth(CONTENT_MAX_WIDTH);

        StackPane contentWrapper = new StackPane(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(32, 28, 40, 28));

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.getStyleClass().add("feature-dashboard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getStyleClass().add("feature-dashboard-view");
        getChildren().addAll(header, scrollPane);
    }

    private VBox createIntroduction() {
        Label title = new Label("Cubaggio");
        title.getStyleClass().add("feature-dashboard-title");
        return new VBox(6, title, introLabel);
    }

    private VBox createSection(String number, String titleText, String descriptionText, TilePane cards) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("feature-dashboard-section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add("feature-dashboard-section-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("feature-dashboard-section-description");

        VBox headingText = new VBox(2, title, description);
        HBox heading = new HBox(12, numberLabel, headingText);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(14, heading, cards);
        section.getStyleClass().add("feature-dashboard-section");
        return section;
    }

    public AppHeader getHeader() { return header; }

    public void setIntroText(String text) {
        introLabel.setText(text == null ? "" : text);
    }

    public Button getCreationButton() { return creationButton; }
    public Button getProductAssignmentButton() { return productAssignmentButton; }
    public Button getPayloadContractButton() { return payloadContractButton; }
}
