package com.orodent.tonv2.features.laboratory.production.view.partial;

import com.orodent.tonv2.core.database.model.Item;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.IntConsumer;
import java.util.function.UnaryOperator;

public class BatchItemRowView extends HBox {

    private final TextField quantityField = new TextField("0");
    private IntConsumer quantityChangeHandler = quantity -> {};

    public BatchItemRowView(Item item) {
        Label code = new Label(item.code());
        code.getStyleClass().add("batch-item-code");
        Label height = new Label(formatHeight(item.heightMm()));
        height.getStyleClass().add("batch-item-detail");
        VBox identity = new VBox(3, code, height);
        identity.setMinWidth(180);
        HBox.setHgrow(identity, Priority.ALWAYS);

        UnaryOperator<TextFormatter.Change> digitsOnly = change ->
                change.getControlNewText().matches("\\d{0,7}") ? change : null;
        quantityField.setTextFormatter(new TextFormatter<>(digitsOnly));
        quantityField.setAlignment(Pos.CENTER);
        quantityField.getStyleClass().add("batch-quantity-field");
        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            int quantity = newValue == null || newValue.isBlank() ? 0 : Integer.parseInt(newValue);
            updateActiveStyle(quantity);
            quantityChangeHandler.accept(quantity);
        });

        Button decrease = createStepButton("−");
        decrease.setOnAction(event -> setQuantity(Math.max(0, getQuantity() - 1)));
        Button increase = createStepButton("+");
        increase.setOnAction(event -> setQuantity(getQuantity() + 1));
        HBox quantityEditor = new HBox(6, decrease, quantityField, increase);
        quantityEditor.setAlignment(Pos.CENTER_RIGHT);

        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("batch-item-row");
        getChildren().addAll(identity, quantityEditor);
    }

    private Button createStepButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("batch-quantity-step");
        button.setMinSize(34, 34);
        return button;
    }

    private String formatHeight(double heightMm) {
        return heightMm == Math.rint(heightMm)
                ? "Altezza " + (int) heightMm + " mm"
                : "Altezza " + heightMm + " mm";
    }

    public void setQuantityChangeHandler(IntConsumer handler) {
        quantityChangeHandler = handler == null ? quantity -> {} : handler;
    }

    public int getQuantity() {
        String text = quantityField.getText();
        return text == null || text.isBlank() ? 0 : Integer.parseInt(text);
    }

    public void setQuantity(int quantity) {
        quantityField.setText(Integer.toString(Math.max(0, quantity)));
    }

    private void updateActiveStyle(int quantity) {
        getStyleClass().remove("batch-item-row-active");
        if (quantity > 0) {
            getStyleClass().add("batch-item-row-active");
        }
    }
}
