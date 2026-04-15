package com.orodent.tonv2.features.cubage.section.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CubageSectionView extends VBox {

    private final AppHeader header;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final Button backButton;

    public CubageSectionView(String pageTitle, String description) {
        this.header = new AppHeader("Cubaggio");
        this.titleLabel = new Label(pageTitle);
        this.descriptionLabel = new Label(description);
        this.backButton = new Button("Torna al menu Cubaggio");

        setSpacing(16);
        setPadding(new Insets(20));

        titleLabel.getStyleClass().add("page-title");
        descriptionLabel.setWrapText(true);

        getChildren().addAll(header, titleLabel, descriptionLabel, backButton);
    }

    public AppHeader getHeader() {
        return header;
    }

    public Button getBackButton() {
        return backButton;
    }
}
