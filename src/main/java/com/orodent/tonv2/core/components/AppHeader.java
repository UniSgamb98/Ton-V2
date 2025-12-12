package com.orodent.tonv2.core.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/*
AppHeader è una componente riutilizzabile dell'applicazione ed è contenuto nella visto di tutte le views presenti.
Di fatto è una classe che svolge la funzione di View, quindi è presente solo il layout. I metodi presenti servono per
esporre ai controller i tasti in modo da poter assegnare loro una funzione.
 */
public class AppHeader extends HBox {

    private final Button homeButton;
    private final Button orderButton;
    private final Button inventoryButton;

    public AppHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("header-title");

        homeButton = new Button("🏠 Home");
        orderButton = new Button("Orders");
        inventoryButton = new Button("Inventario");

        homeButton.getStyleClass().add("header-button");
        orderButton.getStyleClass().add("header-button");
        inventoryButton.getStyleClass().add("header-button");

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(10, 20, 10, 20));
        getStyleClass().add("header-bar");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        this.getChildren().addAll(titleLabel, spacer, inventoryButton, orderButton, homeButton);
    }

    public Button getHomeButton() { return homeButton; }
    public Button getOrdersButton() { return  orderButton; }
    public Button getInventoryButton() {
        return inventoryButton;
    }
}
