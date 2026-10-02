package com.orodent.tonv2.features.registers.home.view.partial;

import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CompositionRegisterView extends VBox {
    private final Label version = value();
    private final Label model = value();
    private final Label height = value();
    private final Label notice = new Label();
    private final FlowPane layers = new FlowPane(12, 12);
    private final HBox heading = new HBox();

    public CompositionRegisterView() {
        Label title = title("Composizione utilizzata");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        heading.getChildren().addAll(title, spacer);
        heading.setAlignment(Pos.CENTER_LEFT);
        FlowPane metrics = new FlowPane(12, 12,
                metric("Versione", version), metric("Modello disco", model), metric("Altezza articolo", height));
        notice.getStyleClass().add("registers-notice");
        notice.setWrapText(true);
        layers.setPrefWrapLength(760);
        getChildren().addAll(heading, metrics, title("Struttura del disco"), notice, layers);
        setSpacing(18);
        getStyleClass().add("registers-tab-content");
    }

    public void setAction(Node action) { heading.getChildren().add(action); }

    public void render(RegistersSearchService.CompositionDetails details) {
        version.setText(details.version() == null ? "Non disponibile" : "Versione " + details.version());
        model.setText("Modello #" + details.blankModelId());
        height.setText(format(details.heightMm()) + " mm");
        notice.setText(details.notice() == null ? "" : details.notice());
        notice.setVisible(details.notice() != null);
        notice.setManaged(details.notice() != null);
        layers.getChildren().clear();
        details.layers().forEach(layer -> layers.getChildren().add(createLayer(layer)));
    }

    private VBox createLayer(RegistersSearchService.CompositionLayerDetails layer) {
        Label layerTitle = new Label("Strato " + layer.layerNumber());
        layerTitle.getStyleClass().add("registers-layer-title");
        Label share = new Label(format(layer.diskPercentage()) + "% del disco");
        share.getStyleClass().add("registers-badge");
        HBox head = new HBox(10, layerTitle, share);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox ingredients = new VBox(7);
        if (layer.ingredients().isEmpty()) {
            ingredients.getChildren().add(muted("Nessuna polvere associata"));
        } else {
            layer.ingredients().forEach(ingredient -> {
                Label name = new Label(ingredient.name());
                name.getStyleClass().add("registers-ingredient-name");
                Label percentage = new Label(format(ingredient.percentage()) + "%");
                percentage.getStyleClass().add("registers-ingredient-percentage");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                ingredients.getChildren().add(new HBox(8, name, spacer, percentage));
            });
        }
        VBox card = new VBox(12, head, ingredients);
        card.setPrefWidth(330);
        card.getStyleClass().add("registers-layer-card");
        return card;
    }

    private VBox metric(String label, Label value) {
        VBox box = new VBox(4, muted(label.toUpperCase()), value);
        box.setPrefWidth(210);
        box.getStyleClass().add("registers-metric-card");
        return box;
    }
    private Label title(String text) { Label l = new Label(text); l.getStyleClass().add("registers-section-title"); return l; }
    private Label muted(String text) { Label l = new Label(text); l.getStyleClass().add("registers-muted"); return l; }
    private static Label value() { Label l = new Label("—"); l.getStyleClass().add("registers-metric-value"); return l; }
    private String format(double value) { return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value); }
}
