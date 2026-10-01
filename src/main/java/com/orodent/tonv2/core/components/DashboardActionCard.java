package com.orodent.tonv2.core.components;

import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class DashboardActionCard extends Button {
    private static final String ICON_RESOURCE = "/icons/dashboard-icons.properties";
    private static final double CONTENT_WIDTH = 218;
    private static final Properties ICONS = loadIcons();

    public DashboardActionCard(String number, String iconName, String titleText, String descriptionText) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("dashboard-card-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        HBox numberRow = new HBox(numberLabel);
        numberRow.setAlignment(Pos.CENTER_LEFT);

        SVGPath iconPath = new SVGPath();
        iconPath.setContent(iconContent(iconName));
        iconPath.getStyleClass().add("dashboard-card-icon");

        StackPane icon = new StackPane(iconPath);
        icon.getStyleClass().add("dashboard-card-icon-container");
        icon.setMinSize(48, 48);
        icon.setPrefSize(48, 48);
        icon.setMaxSize(48, 48);

        Label title = createWrappingLabel(titleText, "dashboard-card-title");
        Label description = createWrappingLabel(descriptionText, "dashboard-card-description");

        VBox graphic = new VBox(10, numberRow, icon, title, description);
        graphic.setPrefWidth(CONTENT_WIDTH);
        graphic.setAlignment(Pos.TOP_CENTER);
        graphic.setFillWidth(true);

        setText(titleText);
        setGraphic(graphic);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(titleText + ". " + descriptionText);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getStyleClass().add("dashboard-action-card");
    }

    private Label createWrappingLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setPrefWidth(CONTENT_WIDTH);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private static String iconContent(String name) {
        String content = ICONS.getProperty(name);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Icona dashboard non trovata: " + name);
        }
        return content;
    }

    private static Properties loadIcons() {
        Properties icons = new Properties();
        try (InputStream stream = Objects.requireNonNull(
                DashboardActionCard.class.getResourceAsStream(ICON_RESOURCE),
                "Risorsa icone dashboard non trovata: " + ICON_RESOURCE
        )) {
            icons.load(stream);
            return icons;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile caricare le icone della dashboard", exception);
        }
    }
}
