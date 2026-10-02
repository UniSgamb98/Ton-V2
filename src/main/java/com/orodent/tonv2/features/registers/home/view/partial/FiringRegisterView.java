package com.orodent.tonv2.features.registers.home.view.partial;

import com.orodent.tonv2.features.registers.home.presentation.RegistersViewState;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FiringRegisterView extends VBox {
    private final Label date = value();
    private final Label furnace = value();
    private final Label temperature = value();
    private final Label notice = new Label();
    private final VBox items = new VBox(7);
    private final Label total = value();

    public FiringRegisterView() {
        FlowPane metrics = new FlowPane(12, 12,
                metric("Data", date), metric("Forno", furnace), metric("Temperatura massima", temperature));
        notice.getStyleClass().add("registers-notice");
        notice.setWrapText(true);
        Label heading = new Label("Articoli presenti nel ciclo");
        heading.getStyleClass().add("registers-section-title");
        VBox totalBox = metric("Quantità totale", total);
        getChildren().addAll(metrics, notice, heading, items, totalBox);
        setSpacing(18);
        getStyleClass().add("registers-tab-content");
    }

    public void render(RegistersViewState.FiringViewState details) {
        date.setText(details.dateText());
        furnace.setText(details.furnaceText());
        temperature.setText(details.temperatureText());
        notice.setText(details.notice() == null ? "" : details.notice());
        notice.setVisible(details.notice() != null);
        notice.setManaged(details.notice() != null);
        items.getChildren().clear();
        details.items().forEach(item -> items.getChildren().add(itemRow(item)));
        total.setText(details.totalText());
    }

    private HBox itemRow(RegistersViewState.FiringItemViewState item) {
        Label code = new Label(item.itemCode());
        code.getStyleClass().add("registers-row-value");
        Label quantity = new Label(item.quantityText());
        quantity.getStyleClass().add("registers-quantity-badge");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, code, spacer, quantity);
        row.getStyleClass().add("registers-data-row");
        return row;
    }

    private VBox metric(String label, Label value) {
        Label heading = new Label(label.toUpperCase());
        heading.getStyleClass().add("registers-muted");
        VBox box = new VBox(4, heading, value);
        box.setPrefWidth(210);
        box.getStyleClass().add("registers-metric-card");
        return box;
    }
    private static Label value() { Label l = new Label("—"); l.getStyleClass().add("registers-metric-value"); return l; }
}
