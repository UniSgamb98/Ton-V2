package com.orodent.tonv2.features.laboratory.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class LaboratoryView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1180;
    private static final double CREATION_CARD_WIDTH = 190;
    private static final double CREATION_CARD_HEIGHT = 198;
    private static final double CREATION_CARD_CONTENT_WIDTH = 156;

    private final AppHeader header;
    private final Button createCompositionButton;
    private final Button createDiskModelButton;
    private final Button archiveDiskModelsButton;
    private final Button archiveCompositionsButton;
    private final Button produceButton;
    private final Button createArticleButton;
    private final Button createFiringCycleButton;
    private final Button presinterButton;

    public LaboratoryView() {
        header = new AppHeader("Laboratorio");

        createDiskModelButton = createCreationCard(
                "1", "disk", "Nuovo modello disco", "Definisci geometria e sovramateriali"
        );
        createCompositionButton = createCreationCard(
                "2", "composition", "Nuova composizione", "Configura materiali e stratificazione"
        );
        createArticleButton = createCreationCard(
                "3", "article", "Nuovo articolo", "Associa articolo, disco e composizione"
        );
        createFiringCycleButton = createCreationCard(
                "4", "firing-cycle", "Nuovo ciclo sinterizzazione", "Imposta temperature, tempi e fasi"
        );

        archiveDiskModelsButton = createHorizontalCard(
                "archive-disk", "Archivio modelli disco", "Consulta e modifica i modelli creati", "archive-card"
        );
        archiveCompositionsButton = createHorizontalCard(
                "archive-composition", "Archivio composizioni", "Consulta e modifica le composizioni", "archive-card"
        );
        produceButton = createHorizontalCard(
                "production", "Produzione", "Avvia e gestisci una produzione batch", "operation-card"
        );
        presinterButton = createHorizontalCard(
                "presintering", "Presinterizzazione", "Prepara e registra la presinterizzazione", "operation-card"
        );

        TilePane creationCards = createTilePane(CREATION_CARD_WIDTH, CREATION_CARD_HEIGHT, 4);
        creationCards.getChildren().addAll(
                createDiskModelButton,
                createCompositionButton,
                createArticleButton,
                createFiringCycleButton
        );

        TilePane archiveCards = createTilePane(360, 104, 2);
        archiveCards.getChildren().addAll(archiveDiskModelsButton, archiveCompositionsButton);

        TilePane operationCards = createTilePane(360, 112, 2);
        operationCards.getChildren().addAll(produceButton, presinterButton);

        VBox content = new VBox(30,
                createIntroduction(),
                createSection("1", "Creazione", "Definisci gli elementi necessari alla produzione.", creationCards),
                createSection("2", "Archivi", "Consulta e modifica gli elementi già configurati.", archiveCards),
                createSection("3", "Lavorazione", "Utilizza le configurazioni nelle attività di laboratorio.", operationCards)
        );
        content.getStyleClass().add("laboratory-content");
        content.setMaxWidth(CONTENT_MAX_WIDTH);

        StackPane contentWrapper = new StackPane(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(32, 28, 40, 28));

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.getStyleClass().add("laboratory-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getStyleClass().add("laboratory-view");
        getChildren().addAll(header, scrollPane);
    }

    private VBox createIntroduction() {
        Label title = new Label("Laboratorio");
        title.getStyleClass().add("laboratory-title");

        Label subtitle = new Label("Configura le risorse, consulta gli archivi e avvia la lavorazione.");
        subtitle.getStyleClass().add("laboratory-subtitle");

        return new VBox(6, title, subtitle);
    }

    private VBox createSection(String number, String titleText, String descriptionText, Region cards) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("section-description");

        VBox headingText = new VBox(2, title, description);
        HBox heading = new HBox(12, numberLabel, headingText);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(14, heading, cards);
        section.getStyleClass().add("laboratory-section");
        return section;
    }

    private TilePane createTilePane(double tileWidth, double tileHeight, int columns) {
        TilePane pane = new TilePane(14, 14);
        pane.setPrefColumns(columns);
        pane.setPrefTileWidth(tileWidth);
        pane.setPrefTileHeight(tileHeight);
        pane.setTileAlignment(Pos.CENTER_LEFT);
        pane.getStyleClass().add("laboratory-card-grid");
        return pane;
    }

    private Button createCreationCard(String number, String iconName, String titleText, String descriptionText) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("card-number");
        numberLabel.setMinSize(28, 28);
        numberLabel.setAlignment(Pos.CENTER);

        HBox numberRow = new HBox(numberLabel);
        numberRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = createIcon(iconName, "lab-card-icon", 43);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setPrefWidth(CREATION_CARD_CONTENT_WIDTH);
        title.setMinHeight(Region.USE_PREF_SIZE);
        title.setAlignment(Pos.CENTER);

        Label description = new Label(descriptionText);
        description.getStyleClass().add("card-description");
        description.setWrapText(true);
        description.setTextOverrun(OverrunStyle.CLIP);
        description.setPrefWidth(CREATION_CARD_CONTENT_WIDTH);
        description.setMinHeight(Region.USE_PREF_SIZE);
        description.setAlignment(Pos.CENTER);

        VBox graphic = new VBox(8, numberRow, icon, title, description);
        graphic.setPrefWidth(CREATION_CARD_CONTENT_WIDTH);
        graphic.setAlignment(Pos.TOP_CENTER);
        graphic.setFillWidth(true);

        return configureCardButton(new Button(titleText, graphic), titleText, "creation-card");
    }

    private Button createHorizontalCard(
            String iconName,
            String titleText,
            String descriptionText,
            String cardStyleClass
    ) {
        StackPane icon = createIcon(iconName, "lab-card-icon", 38);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("card-description");
        description.setWrapText(true);

        VBox text = new VBox(4, title, description);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("card-arrow");

        HBox graphic = new HBox(16, icon, text, arrow);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setFillHeight(true);

        return configureCardButton(new Button(titleText, graphic), titleText, cardStyleClass);
    }

    private StackPane createIcon(String iconName, String styleClass, double size) {
        SVGPath path = LaboratoryIcons.create(iconName);
        path.getStyleClass().add(styleClass);

        StackPane container = new StackPane(path);
        container.getStyleClass().add("card-icon-container");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }

    private Button configureCardButton(Button button, String accessibleText, String styleClass) {
        button.getStyleClass().addAll("lab-card", styleClass);
        button.setAccessibleRole(AccessibleRole.BUTTON);
        button.setAccessibleText(accessibleText);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public Button getCreateCompositionButton() { return createCompositionButton; }
    public Button getCreateDiskModelButton() { return createDiskModelButton; }
    public Button getArchiveDiskModelsButton() { return archiveDiskModelsButton; }
    public Button getArchiveCompositionsButton() { return archiveCompositionsButton; }
    public Button getProduceButton() { return produceButton; }
    public Button getCreateArticleButton() { return createArticleButton; }
    public Button getCreateFiringCycleButton() { return createFiringCycleButton; }
    public Button getPresinterButton() { return presinterButton; }
    public AppHeader getHeader() { return header; }
}
