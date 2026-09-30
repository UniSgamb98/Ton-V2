package com.orodent.tonv2.features.registers.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RegistersView extends VBox {
    private final AppHeader header;
    private final ComboBox<String> articleComboBox;
    private final ComboBox<String> lotComboBox;
    private final Button searchButton;
    private final TabPane historyTabs;
    private final TextArea compositionSummaryArea;
    private final TextArea firingSummaryArea;
    private final TextArea documentsPreviewArea;
    private final Button buildCompositionDocumentButton;
    private final Button buildFiringDocumentButton;
    private final ProgressIndicator progressIndicator;
    private final Label statusLabel;

    public RegistersView() {
        header = new AppHeader("Registri");
        articleComboBox = new ComboBox<>();
        lotComboBox = new ComboBox<>();
        searchButton = new Button("Cerca");
        progressIndicator = new ProgressIndicator();
        statusLabel = new Label();
        progressIndicator.setMaxSize(22, 22);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        articleComboBox.setEditable(true);
        lotComboBox.setEditable(true);
        articleComboBox.setPromptText("Codice articolo");
        lotComboBox.setPromptText("Codice lotto");
        articleComboBox.setPrefWidth(260);
        lotComboBox.setPrefWidth(260);

        setSpacing(20);
        setPadding(new Insets(20));

        VBox articleBox = new VBox(8, new Label("Articolo"), articleComboBox);
        VBox lotBox = new VBox(8, new Label("Lotto"), lotComboBox);
        HBox filtersBox = new HBox(20, articleBox, lotBox, searchButton);
        HBox statusBox = new HBox(8, progressIndicator, statusLabel);

        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);

        historyTabs = new TabPane();

        compositionSummaryArea = buildReadOnlyArea("Qui verrà visualizzata la storia composizione del prodotto.");
        firingSummaryArea = buildReadOnlyArea("Qui verrà visualizzata la storia firing del lotto selezionato.");
        documentsPreviewArea = buildReadOnlyArea("Qui verranno mostrati i documenti ricostruiti da template.");

        historyTabs.getTabs().add(createTab("Composizione", compositionSummaryArea));
        historyTabs.getTabs().add(createTab("Firing", firingSummaryArea));
        historyTabs.getTabs().add(createTab("Documenti", documentsPreviewArea));

        buildCompositionDocumentButton = new Button("Rigenera documento composizione");
        buildFiringDocumentButton = new Button("Rigenera documento firing");
        buildFiringDocumentButton.setDisable(true);
        buildFiringDocumentButton.setTooltip(new Tooltip("Funzionalità non disponibile"));
        HBox actionsBox = new HBox(12, buildCompositionDocumentButton, buildFiringDocumentButton);

        getChildren().addAll(header, filtersBox, statusBox, separator, historyTabs, actionsBox);
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

    public ComboBox<String> getArticleComboBox() {
        return articleComboBox;
    }

    public ComboBox<String> getLotComboBox() {
        return lotComboBox;
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

    public void showSearchLoading() {
        setPrimaryActionsDisabled(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        showStatus("Ricerca in corso...", false);
    }

    public void showSearchResult(RegistersSearchService.SearchResult result) {
        setPrimaryActionsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        compositionSummaryArea.setText(result.compositionOutput());
        firingSummaryArea.setText(result.firingOutput());
        documentsPreviewArea.setText(result.documentsOutput());
        showStatus(result.success() ? "Ricerca completata." : "Ricerca completata con segnalazioni.", !result.success());
    }

    public void showSearchError(String message) {
        setPrimaryActionsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        showStatus(message, true);
    }

    public void showSuggestionError() {
        if (!searchButton.isDisabled()) {
            showStatus("Errore durante il caricamento dei suggerimenti.", true);
        }
    }

    public void showDocumentGenerationLoading() {
        setPrimaryActionsDisabled(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        showStatus("Generazione documento in corso...", false);
    }

    public void showDocumentGenerationSuccess() {
        setPrimaryActionsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        showStatus("Documento generato e aperto nel browser.", false);
    }

    public void showDocumentGenerationError(String message) {
        setPrimaryActionsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        String safeMessage = message == null || message.isBlank()
                ? "Errore durante la generazione del documento."
                : message;
        showStatus(safeMessage, true);
    }

    private void setPrimaryActionsDisabled(boolean disabled) {
        searchButton.setDisable(disabled);
        articleComboBox.setDisable(disabled);
        lotComboBox.setDisable(disabled);
        buildCompositionDocumentButton.setDisable(disabled);
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b00020;" : "");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
