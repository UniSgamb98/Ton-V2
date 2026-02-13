package com.orodent.tonv2.features.laboratory.view;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.view.partial.LayerEditorView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class CreateCompositionView extends BorderPane {

    /* =======================
       COMPONENTI UI
       ======================= */

    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final TextArea notesArea = new TextArea();
    private final VBox layersBox = new VBox(10);

    private final Button addLayerBtn = new Button("Aggiungi layer");
    private final Button saveBtn = new Button("Salva composizione");

    /* =======================
       STATO UI (DRAFT)
       ======================= */

    private final List<LayerDraft> layers = new ArrayList<>();
    private List<Powder> availablePowders = new ArrayList<>();

    /* =======================
       COSTRUTTORE
       ======================= */

    public CreateCompositionView() {
        buildLayout();
    }

    public void setAvailablePowders(List<Powder> powders) {
        this.availablePowders = powders;
    }

    /* =======================
       COSTRUZIONE UI
       ======================= */

    private void buildLayout() {

        /* ---- TOP: Product + Notes ---- */

        productSelector.setPromptText("Seleziona prodotto");

        notesArea.setPromptText("Note sulla composizione...");
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(3);

        VBox topBox = new VBox(10,
                new Label("Prodotto"),
                productSelector,
                new Label("Note"),
                notesArea
        );
        topBox.setPadding(new Insets(10));

        /* ---- CENTER: Layers ---- */

        ScrollPane scroll = new ScrollPane(layersBox);
        scroll.setFitToWidth(true);

        VBox centerBox = new VBox(10,
                new Label("Strati"),
                scroll,
                addLayerBtn
        );
        centerBox.setPadding(new Insets(10));
        centerBox.setMaxWidth(900);

        StackPane centeredCenterBox = new StackPane(centerBox);
        centeredCenterBox.setPadding(new Insets(0, 10, 0, 10));
        StackPane.setAlignment(centerBox, Pos.TOP_CENTER);

        /* ---- BOTTOM: Save ---- */

        HBox bottomBox = new HBox(saveBtn);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        /* ---- BorderPane ---- */

        setTop(topBox);
        setCenter(centeredCenterBox);
        setBottom(bottomBox);

        /* ---- Actions ---- */

        addLayerBtn.setOnAction(e -> addLayer());
    }

    /* =======================
       LOGICA UI (solo draft)
       ======================= */

    private void addLayer() {
        LayerDraft layerDraft = new LayerDraft(layers.size()+1);
        layers.add(layerDraft);

        LayerEditorView layerView = new LayerEditorView(layerDraft, availablePowders);

        layerView.setOnRemove(() -> {
            layers.remove(layerDraft);
            layersBox.getChildren().remove(layerView);
            renumberLayers();
        });

        layersBox.getChildren().add(layerView);
    }

    public void renumberLayers() {

        for (int i = 0; i < layers.size(); i++) {

            int number = i + 1;

            // aggiorna il draft
            LayerDraft draft = layers.get(i);
            draft.setLayerNumber(number);

            // aggiorna la view (stesso ordine nel VBox)
            Node node = layersBox.getChildren().get(i);
            if (node instanceof LayerEditorView view) {
                view.setLayerNumber(number);
            }
        }
    }

    /* =======================
       GETTER USATI DAL CONTROLLER
       ======================= */

    public ComboBox<Product> getProductSelector() {
        return productSelector;
    }

    public String getNotes() {
        String text = notesArea.getText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public List<LayerDraft> getLayers() {
        return List.copyOf(layers);
    }

    public Button getSaveButton() {
        return saveBtn;
    }
}

