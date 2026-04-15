package com.orodent.tonv2.features.cubage.creation.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CubageCreationView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label titleLabel = new Label("Gestione Calcoli Cubaggio");
    private final Label infoLabel = new Label("Anteprima UI - logica non ancora collegata");

    private final ComboBox<String> payloadSelector = new ComboBox<>();
    private final Button selectLegacyPayloadButton = new Button("Seleziona payload legacy");
    private final TextArea payloadPreviewArea = new TextArea();
    private final TextArea formulaBuilderArea = new TextArea();
    private final TextArea resultsArea = new TextArea();

    public CubageCreationView() {
        setSpacing(16);
        setPadding(new Insets(20));

        titleLabel.getStyleClass().add("page-title");
        infoLabel.getStyleClass().add("text-muted");
        infoLabel.setWrapText(true);

        payloadSelector.setPromptText("Seleziona payload attivo");
        payloadSelector.getItems().addAll(
                "PROJECT2_PAYLOAD v2 (attivo)",
                "PROJECT2_PAYLOAD v1"
        );
        payloadSelector.getSelectionModel().selectFirst();

        payloadPreviewArea.setEditable(false);
        payloadPreviewArea.setWrapText(true);
        payloadPreviewArea.setText("""
                Payload in uso (placeholder):
                - input_1 (DECIMAL, mm3)
                - input_2 (DECIMAL, g/cm3)
                - input_3 (DECIMAL, g)
                """);

        formulaBuilderArea.setWrapText(true);
        formulaBuilderArea.setPromptText("Area creazione formule (placeholder UI)");
        formulaBuilderArea.setText("""
                Esempio formula:
                calc_volume_density_ratio = input_1 / input_2
                """);

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setText("""
                Risultati (placeholder):
                - Output formula 1
                - Output formula 2
                """);

        VBox leftPanel = buildLeftPanel();
        VBox centerPanel = buildCenterPanel();
        VBox rightPanel = buildRightPanel();

        HBox contentRow = new HBox(16, leftPanel, centerPanel, rightPanel);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        leftPanel.setPrefWidth(260);
        centerPanel.setPrefWidth(420);
        rightPanel.setPrefWidth(320);

        getChildren().addAll(header, titleLabel, infoLabel, contentRow);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setInfoText(String text) {
        infoLabel.setText(text == null ? "" : text);
    }

    private VBox buildLeftPanel() {
        Label panelTitle = new Label("Selezione Payload");
        panelTitle.getStyleClass().add("section-title");

        VBox panel = new VBox(10, panelTitle, payloadSelector, selectLegacyPayloadButton);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("card");
        return panel;
    }

    private VBox buildCenterPanel() {
        Label payloadTitle = new Label("Payload");
        payloadTitle.getStyleClass().add("section-title");
        Label formulasTitle = new Label("Creazione Formule");
        formulasTitle.getStyleClass().add("section-title");

        BorderPane panel = new BorderPane();
        panel.setTop(new VBox(8, payloadTitle, payloadPreviewArea));
        panel.setCenter(new VBox(8, formulasTitle, formulaBuilderArea));
        BorderPane.setMargin(payloadPreviewArea, new Insets(0, 0, 8, 0));

        VBox wrapper = new VBox(panel);
        wrapper.setPadding(new Insets(12));
        wrapper.getStyleClass().add("card");
        VBox.setVgrow(panel, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildRightPanel() {
        Label panelTitle = new Label("Risultati");
        panelTitle.getStyleClass().add("section-title");

        VBox panel = new VBox(10, panelTitle, resultsArea);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("card");
        VBox.setVgrow(resultsArea, Priority.ALWAYS);
        return panel;
    }
}
