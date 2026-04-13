package com.orodent.tonv2.features.laboratory.firingprogram.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class FiringProgramView extends VBox {

    private final AppHeader header;
    private final TextField programNameField;
    private final VBox stepsBox;
    private final Button addStepButton;
    private final Button saveButton;
    private final Button backButton;
    private final Label feedbackLabel;
    private final List<StepRow> stepRows = new ArrayList<>();

    public FiringProgramView() {
        header = new AppHeader("Nuovo Ciclo Sinterizzazione");
        programNameField = new TextField();
        programNameField.setPromptText("Nome programma");

        Label programNameLabel = new Label("Nome programma");
        VBox programNameBox = new VBox(6, programNameLabel, programNameField);

        stepsBox = new VBox(8);
        stepsBox.setStyle("-fx-text-fill: #000000;");
        ScrollPane stepsScroll = new ScrollPane(stepsBox);
        stepsScroll.setFitToWidth(true);
        stepsScroll.setPrefViewportHeight(420);

        addStepButton = new Button("Aggiungi Step");
        saveButton = new Button("Salva Programma");
        backButton = new Button("Indietro");

        feedbackLabel = new Label();

        HBox actions = new HBox(10, backButton, addStepButton, saveButton);

        setSpacing(14);
        setPadding(new Insets(20));
        getChildren().addAll(header, programNameBox, stepsScroll, actions, feedbackLabel);
        VBox.setVgrow(stepsScroll, Priority.ALWAYS);
    }

    public StepRow addStepRow() {
        StepRow row = new StepRow();
        stepRows.add(row);
        stepsBox.getChildren().add(row.getRoot());
        return row;
    }

    public void removeStepRow(StepRow row) {
        stepRows.remove(row);
        stepsBox.getChildren().remove(row.getRoot());
    }

    public void refreshStepLabels() {
        for (int i = 0; i < stepRows.size(); i++) {
            stepRows.get(i).setStepIndex(i + 1);
        }
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getProgramNameField() {
        return programNameField;
    }

    public Button getAddStepButton() {
        return addStepButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public Label getFeedbackLabel() {
        return feedbackLabel;
    }

    public List<StepRow> getStepRows() {
        return List.copyOf(stepRows);
    }

    public static class StepRow {
        private final HBox root;
        private final Label titleLabel;
        private final TextField targetTemperatureField;
        private final TextField rampTimeField;
        private final TextField holdTimeField;
        private final Button removeButton;

        public StepRow() {
            titleLabel = new Label("Step 01:");
            titleLabel.setStyle("-fx-text-fill: #000000;");
            targetTemperatureField = new TextField();
            targetTemperatureField.setPromptText("Temperatura di arrivo");
            targetTemperatureField.setStyle("-fx-text-fill: #000000; -fx-prompt-text-fill: #444444;");

            rampTimeField = new TextField();
            rampTimeField.setPromptText("Tempo di Rampa (min)");
            rampTimeField.setStyle("-fx-text-fill: #000000; -fx-prompt-text-fill: #444444;");

            holdTimeField = new TextField();
            holdTimeField.setPromptText("Tempo di mantenuta (min)");
            holdTimeField.setStyle("-fx-text-fill: #000000; -fx-prompt-text-fill: #444444;");

            removeButton = new Button("Rimuovi");

            root = new HBox(8, titleLabel, targetTemperatureField, rampTimeField, holdTimeField, removeButton);
            HBox.setHgrow(targetTemperatureField, Priority.ALWAYS);
            HBox.setHgrow(rampTimeField, Priority.ALWAYS);
            HBox.setHgrow(holdTimeField, Priority.ALWAYS);
        }

        public HBox getRoot() {
            return root;
        }

        public void setStepIndex(int stepIndex) {
            titleLabel.setText(String.format("Step %02d:", stepIndex));
        }

        public TextField getTargetTemperatureField() {
            return targetTemperatureField;
        }

        public TextField getRampTimeField() {
            return rampTimeField;
        }

        public TextField getHoldTimeField() {
            return holdTimeField;
        }

        public Button getRemoveButton() {
            return removeButton;
        }
    }
}
