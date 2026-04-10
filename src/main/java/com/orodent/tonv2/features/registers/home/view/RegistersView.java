package com.orodent.tonv2.features.registers.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

public class RegistersView extends VBox {
    private final AppHeader header;

    public RegistersView() {
        header = new AppHeader("Registri");

        setSpacing(20);
        setPadding(new Insets(20));

        getChildren().add(header);
    }

    public AppHeader getHeader() {
        return header;
    }
}
