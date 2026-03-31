package com.orodent.tonv2.features.laboratory.composition.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.composition.view.partial.LayerEditorView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class CreateCompositionView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Nuova Composizione");
    private final BorderPane content = new BorderPane();

    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final ComboBox<String> lineSelector = new ComboBox<>();
    private final ComboBox<BlankModel> blankModelSelector = new ComboBox<>();
    private final Button loadLatestVersionBtn = new Button("↵");
    private final Label loadLatestVersionLabel = new Label("carica ultima versione");
    private final HBox loadLatestVersionBox = new HBox(6, loadLatestVersionBtn, loadLatestVersionLabel);
    private final TextArea notesArea = new TextArea();
    private final VBox layersBox = new VBox(10);

    private final Button saveBtn = new Button("Salva composizione");
    private final Button backBtn = new Button("Indietro");

    private final List<LayerDraft> layers = new ArrayList<>();
    private List<Powder> availablePowders = new ArrayList<>();

    public CreateCompositionView() {
        setSpacing(20);
        setPadding(new Insets(20));

        buildLayout();

        getChildren().addAll(header, content);
    }

    public void setAvailablePowders(List<Powder> powders) {
        this.availablePowders = powders;
    }

    private void buildLayout() {
        productSelector.setPromptText("Seleziona prodotto");
        lineSelector.setPromptText("Seleziona linea");
        blankModelSelector.setPromptText("Seleziona modello blank");

        loadLatestVersionBtn.setFocusTraversable(false);
        loadLatestVersionBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8 2 8;");
        loadLatestVersionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4b5563;");
        loadLatestVersionBox.setAlignment(Pos.CENTER_LEFT);
        setLoadLatestVersionVisible(false);

        notesArea.setPromptText("Note sulla composizione...");
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(3);

        VBox productBox = new VBox(6,
                new Label("Prodotto"),
                productSelector,
                loadLatestVersionBox
        );

        VBox blankModelBox = new VBox(6,
                new Label("Modello blank"),
                blankModelSelector
        );

        VBox lineBox = new VBox(6,
                new Label("Linea"),
                lineSelector
        );

        HBox selectorsBox = new HBox(16, productBox, lineBox, blankModelBox);
        selectorsBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(productBox, Priority.ALWAYS);
        HBox.setHgrow(lineBox, Priority.ALWAYS);
        HBox.setHgrow(blankModelBox, Priority.ALWAYS);
        productBox.setMaxWidth(Double.MAX_VALUE);
        lineBox.setMaxWidth(Double.MAX_VALUE);
        blankModelBox.setMaxWidth(Double.MAX_VALUE);
        productSelector.setMaxWidth(Double.MAX_VALUE);
        lineSelector.setMaxWidth(Double.MAX_VALUE);
        blankModelSelector.setMaxWidth(Double.MAX_VALUE);

        VBox topBox = new VBox(10,
                selectorsBox,
                new Label("Note"),
                notesArea
        );
        topBox.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(layersBox);
        scroll.setFitToWidth(true);

        VBox centerBox = new VBox(10,
                new Label("Strati"),
                scroll
        );
        centerBox.setPadding(new Insets(10));
        centerBox.setMaxWidth(900);

        StackPane centeredCenterBox = new StackPane(centerBox);
        centeredCenterBox.setPadding(new Insets(0, 10, 0, 10));
        StackPane.setAlignment(centerBox, Pos.TOP_CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        HBox bottomBox = new HBox(10, backBtn, spacer, saveBtn);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        content.setTop(topBox);
        content.setCenter(centeredCenterBox);
        content.setBottom(bottomBox);
    }

    private void createLayerView(LayerDraft layerDraft) {
        LayerEditorView layerView = new LayerEditorView(layerDraft, availablePowders);
        // layer count fissato dal modello blank: non abilitiamo rimozione layer
        layerView.setLayerRemovalEnabled(false);
        layerView.setOnRemove(() -> {});
        layersBox.getChildren().add(layerView);
    }

    public void setLayerCount(int layerCount) {
        layers.clear();
        layersBox.getChildren().clear();

        for (int i = 1; i <= layerCount; i++) {
            LayerDraft layerDraft = new LayerDraft(i);
            layers.add(layerDraft);
            createLayerView(layerDraft);
        }
    }

    public void renumberLayers() {
        for (int i = 0; i < layers.size(); i++) {
            int number = i + 1;
            LayerDraft draft = layers.get(i);
            draft.setLayerNumber(number);

            Node node = layersBox.getChildren().get(i);
            if (node instanceof LayerEditorView view) {
                view.setLayerNumber(number);
            }
        }
    }

    public void configureEditMode(boolean editMode) {
        if (editMode) {
            header.setTitle("Laboratorio - Modifica Composizione");
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        } else {
            header.setTitle("Laboratorio - Nuova Composizione");
            backBtn.setVisible(false);
            backBtn.setManaged(false);
        }
    }


    public void setLineSelectorLocked(boolean locked) {
        lineSelector.setDisable(locked);
    }

    public ComboBox<Product> getProductSelector() {
        return productSelector;
    }

    public ComboBox<BlankModel> getBlankModelSelector() {
        return blankModelSelector;
    }

    public ComboBox<String> getLineSelector() {
        return lineSelector;
    }

    public String getNotes() {
        String text = notesArea.getText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public void setNotes(String notes) {
        notesArea.setText(notes == null ? "" : notes);
    }

    public List<LayerDraft> getLayers() {
        return List.copyOf(layers);
    }

    public void replaceLayers(List<LayerDraft> newLayers) {
        layers.clear();
        layersBox.getChildren().clear();

        for (LayerDraft newLayer : newLayers) {
            LayerDraft draft = new LayerDraft(newLayer.layerNumber());
            draft.setNotes(newLayer.notes());
            newLayer.ingredients().forEach(ingredient ->
                    draft.ingredients().add(new IngredientDraft(
                            ingredient.powderId(),
                            ingredient.percentage()
                    ))
            );

            layers.add(draft);
            createLayerView(draft);
        }

        renumberLayers();
    }

    public Button getSaveButton() {
        return saveBtn;
    }

    public Button getBackButton() {
        return backBtn;
    }

    public Button getLoadLatestVersionButton() {
        return loadLatestVersionBtn;
    }

    public void setLoadLatestVersionVisible(boolean visible) {
        loadLatestVersionBox.setVisible(visible);
        loadLatestVersionBox.setManaged(visible);
    }

    public AppHeader getHeader() {
        return header;
    }
}
