package com.orodent.tonv2.features.laboratory.view;

import com.orodent.tonv2.core.database.model.Product;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;

public class CreateCompositionView extends BorderPane {

    private final Label title;
    private final ComboBox<Product> itemSelector;

    private final VBox layersBox;
    private final Button addLayerBtn;
    private final Button saveBtn;
    private final Button cancelBtn;

    public CreateCompositionView() {

        // HEADER
        title = new Label("Crea Nuova Composizione");
        title.getStyleClass().add("page-title");

        itemSelector = new ComboBox<>();

        itemSelector.setPromptText("Seleziona Articolo");

        HBox selectors = new HBox(10, new Label("Articolo:"), itemSelector);
        selectors.setPadding(new Insets(10));

        VBox header = new VBox(10, title, selectors);
        header.setPadding(new Insets(20));

        // AREA CENTRALE: LISTA STRATI
        layersBox = new VBox(12);
        layersBox.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(layersBox);
        scrollPane.setFitToWidth(true);

        // FOOTER
        addLayerBtn = new Button("Aggiungi Strato");
        saveBtn = new Button("Salva Composizione");
        cancelBtn = new Button("Annulla");

        HBox footer = new HBox(10, addLayerBtn, saveBtn, cancelBtn);
        footer.setPadding(new Insets(20));
        footer.getStyleClass().add("footer-buttons");

        // ASSEMBLA LA VIEW
        this.setTop(header);
        this.setCenter(scrollPane);
        this.setBottom(footer);

        this.getStyleClass().add("create-composition-root");
    }

    // GETTERS
    public ComboBox<Product> getProductSelector() { return itemSelector; }

    public VBox getLayersBox() { return layersBox; }

    public Button getAddLayerBtn() { return addLayerBtn; }
    public Button getSaveBtn() { return saveBtn; }
    public Button getCancelBtn() { return cancelBtn; }

    // Metodo utile al controller per aggiungere un layer
    public void addLayer(Node node) {
        layersBox.getChildren().add(node);
    }
}
