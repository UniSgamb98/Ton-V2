package com.orodent.tonv2.features.registers.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RegistersView extends VBox {
    private final AppHeader header;
    private final TextField articleField;
    private final TextField lotField;
    private final Button searchButton;
    private final TabPane historyTabs;
    private final TextArea compositionSummaryArea;
    private final TextArea firingSummaryArea;
    private final TextArea documentsPreviewArea;
    private final Button buildCompositionDocumentButton;
    private final Button buildFiringDocumentButton;

    public RegistersView() {
        header = new AppHeader("Registri");
        articleField = new TextField();
        lotField = new TextField();
        searchButton = new Button("Cerca");

        articleField.setPromptText("Codice articolo");
        lotField.setPromptText("Codice lotto");

        setSpacing(20);
        setPadding(new Insets(20));

        VBox articleBox = new VBox(8, new Label("Articolo"), articleField);
        VBox lotBox = new VBox(8, new Label("Lotto"), lotField);
        HBox filtersBox = new HBox(20, articleBox, lotBox, searchButton);

        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);

        historyTabs = new TabPane();

        compositionSummaryArea = buildReadOnlyArea("Qui verrà visualizzata la storia composizione del prodotto.");
        firingSummaryArea = buildReadOnlyArea("Qui verrà visualizzata la storia firing del lotto selezionato.");
        documentsPreviewArea = buildReadOnlyArea("Qui verranno mostrati i documenti ricostruiti da template.");

        historyTabs.getTabs().add(createTab("Composizione", compositionSummaryArea));
        historyTabs.getTabs().add(createTab("Firing", firingSummaryArea));
        historyTabs.getTabs().add(createTab("Documenti", documentsPreviewArea));

        buildCompositionDocumentButton = new Button("Genera documento composizione");
        buildFiringDocumentButton = new Button("Genera documento firing");
        HBox actionsBox = new HBox(12, buildCompositionDocumentButton, buildFiringDocumentButton);

        getChildren().addAll(header, filtersBox, separator, historyTabs, actionsBox);
        VBox.setVgrow(historyTabs, Priority.ALWAYS);
    }

    private Tab createTab(String title, TextArea contentArea) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        tab.setContent(scrollPane);
        return tab;
    }

    private TextArea buildReadOnlyArea(String placeholder) {
        TextArea area = new TextArea(placeholder);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(14);
        return area;
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getArticleField() {
        return articleField;
    }

    public TextField getLotField() {
        return lotField;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public TabPane getHistoryTabs() {
        return historyTabs;
    }

    public TextArea getCompositionSummaryArea() {
        return compositionSummaryArea;
    }

    public TextArea getFiringSummaryArea() {
        return firingSummaryArea;
    }

    public TextArea getDocumentsPreviewArea() {
        return documentsPreviewArea;
    }

    public Button getBuildCompositionDocumentButton() {
        return buildCompositionDocumentButton;
    }

    public Button getBuildFiringDocumentButton() {
        return buildFiringDocumentButton;
    }
}
