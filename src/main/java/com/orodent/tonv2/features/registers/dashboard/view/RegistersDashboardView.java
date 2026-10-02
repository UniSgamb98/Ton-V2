package com.orodent.tonv2.features.registers.dashboard.view;

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

public final class RegistersDashboardView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1180;

    private final AppHeader header = new AppHeader("Registri");
    private final Button producedDisksArchiveButton = new DashboardActionCard(
            "1",
            "registers-archive",
            "Archivio dischi prodotti",
            "Consulta composizione, sinterizzazione e documenti associati ai lotti prodotti"
    );
    private final Button salesUpdateButton = new DashboardActionCard(
            "2",
            "registers-sales-update",
            "Aggiorna vendite",
            "Importa il riepilogo CSV per aggiornare quantità e valori di vendita"
    );

    public RegistersDashboardView() {
        VBox content = new VBox(30,
                createIntroduction(),
                createSection(
                        "1",
                        "Consultazione produzione",
                        "Ricerca i dischi prodotti e ricostruisci le informazioni del relativo lotto.",
                        producedDisksArchiveButton
                ),
                createSection(
                        "2",
                        "Aggiornamento dati",
                        "Carica i dati commerciali provenienti dal gestionale.",
                        salesUpdateButton
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
        Label title = new Label("Registri");
        title.getStyleClass().add("feature-dashboard-title");
        Label subtitle = new Label("Consulta la produzione e mantieni aggiornati i dati di vendita.");
        subtitle.getStyleClass().add("feature-dashboard-subtitle");
        return new VBox(6, title, subtitle);
    }

    private VBox createSection(
            String number,
            String titleText,
            String descriptionText,
            Button action
    ) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("feature-dashboard-section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add("feature-dashboard-section-title");
        Label description = new Label(descriptionText);
        description.getStyleClass().add("feature-dashboard-section-description");
        description.setWrapText(true);

        HBox heading = new HBox(12, numberLabel, new VBox(2, title, description));
        heading.setAlignment(Pos.CENTER_LEFT);

        TilePane cards = new TilePane(16, 16, action);
        cards.setPrefColumns(1);
        cards.setPrefTileWidth(252);
        cards.setPrefTileHeight(210);
        cards.setTileAlignment(Pos.CENTER_LEFT);
        cards.getStyleClass().add("feature-dashboard-card-grid");

        VBox section = new VBox(14, heading, cards);
        section.getStyleClass().add("feature-dashboard-section");
        return section;
    }

    public AppHeader getHeader() { return header; }
    public Button getProducedDisksArchiveButton() { return producedDisksArchiveButton; }
    public Button getSalesUpdateButton() { return salesUpdateButton; }
}
