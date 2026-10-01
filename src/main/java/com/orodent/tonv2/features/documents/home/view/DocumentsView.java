package com.orodent.tonv2.features.documents.home.view;

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

public class DocumentsView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1180;

    private final AppHeader header;
    private final Button createDocumentButton;
    private final Button archiveButton;
    private final Button searchButton;

    public DocumentsView() {
        header = new AppHeader("Documentazione");

        createDocumentButton = new DashboardActionCard(
                "1", "document-new", "Nuovo documento", "Crea e configura un nuovo template documentale"
        );
        archiveButton = new DashboardActionCard(
                "2", "document-archive", "Archivio template", "Consulta, modifica e organizza i template creati"
        );
        searchButton = new DashboardActionCard(
                "3", "document-search", "Ricerca documenti", "Cerca e consulta la documentazione disponibile"
        );

        TilePane cards = new TilePane(16, 16);
        cards.setPrefColumns(3);
        cards.setPrefTileWidth(252);
        cards.setPrefTileHeight(210);
        cards.setTileAlignment(Pos.CENTER_LEFT);
        cards.getStyleClass().add("feature-dashboard-card-grid");
        cards.getChildren().addAll(createDocumentButton, archiveButton, searchButton);

        VBox content = new VBox(30,
                createIntroduction(),
                createSection(
                        "1",
                        "Strumenti documentali",
                        "Crea i template, gestisci l'archivio e consulta i documenti.",
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
        Label title = new Label("Documentazione");
        title.getStyleClass().add("feature-dashboard-title");

        Label subtitle = new Label("Crea, organizza e consulta i documenti utilizzati dall'applicazione.");
        subtitle.getStyleClass().add("feature-dashboard-subtitle");
        return new VBox(6, title, subtitle);
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
    public Button getCreateDocumentButton() { return createDocumentButton; }
    public Button getArchiveButton() { return archiveButton; }
    public Button getSearchButton() { return searchButton; }
}
