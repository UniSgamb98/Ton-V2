package com.orodent.tonv2.features.registers.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.partial.CompositionRegisterView;
import com.orodent.tonv2.features.registers.home.view.partial.DocumentsRegisterView;
import com.orodent.tonv2.features.registers.home.view.partial.FiringRegisterView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RegistersView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1120;

    private final AppHeader header = new AppHeader("Registri");
    private final ComboBox<String> articleComboBox = new ComboBox<>();
    private final ComboBox<String> lotComboBox = new ComboBox<>();
    private final Button searchButton = new Button("Cerca nel registro");
    private final Button clearButton = new Button("Azzera");
    private final TabPane historyTabs = new TabPane();
    private final CompositionRegisterView compositionView = new CompositionRegisterView();
    private final FiringRegisterView firingView = new FiringRegisterView();
    private final DocumentsRegisterView documentsView = new DocumentsRegisterView();
    private final Button buildCompositionDocumentButton = new Button("Rigenera documento composizione");
    private final Button buildFiringDocumentButton = new Button("Rigenera documento firing");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final VBox emptyState = createEmptyState();
    private final VBox resultsContent = new VBox(18);
    private final Label identityItem = new Label();
    private final Label identityLot = new Label();
    private final Label identityComposition = new Label();
    private final Label identityFiring = new Label();

    public RegistersView() {
        buildLayout();
        getStyleClass().add("registers-view");
    }

    private void buildLayout() {
        configureSearchField(articleComboBox, "Cerca o seleziona un articolo…");
        configureSearchField(lotComboBox, "Cerca o seleziona un lotto…");
        searchButton.getStyleClass().add("registers-primary-action");
        clearButton.getStyleClass().add("registers-secondary-action");

        FlowPane fields = new FlowPane(14, 12,
                createField("Articolo", articleComboBox),
                createField("Lotto", lotComboBox),
                new VBox(6, invisibleLabel(), searchButton),
                new VBox(6, invisibleLabel(), clearButton));
        fields.setAlignment(Pos.BOTTOM_LEFT);
        fields.setPrefWrapLength(1000);

        Label searchTitle = new Label("Ricerca nel registro");
        searchTitle.getStyleClass().add("registers-card-title");
        Label searchHint = new Label("Articolo e lotto si filtrano automaticamente in base ai dati inseriti.");
        searchHint.getStyleClass().add("registers-muted");
        VBox searchCard = new VBox(14, searchTitle, fields, searchHint);
        searchCard.getStyleClass().add("registers-search-card");

        historyTabs.getTabs().addAll(
                createTab("Composizione", compositionView),
                createTab("Sinterizzazione", firingView),
                createTab("Documenti", documentsView));
        historyTabs.getStyleClass().add("registers-tabs");
        historyTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        buildCompositionDocumentButton.getStyleClass().add("registers-document-action");
        compositionView.setAction(buildCompositionDocumentButton);
        buildFiringDocumentButton.setDisable(true);
        buildFiringDocumentButton.setVisible(false);
        buildFiringDocumentButton.setManaged(false);

        resultsContent.getChildren().addAll(createIdentityCard(), historyTabs);
        resultsContent.setVisible(false);
        resultsContent.setManaged(false);

        progressIndicator.setMaxSize(22, 22);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.getStyleClass().add("registers-status");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        HBox statusBox = new HBox(9, progressIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Registri di produzione");
        title.getStyleClass().add("registers-page-title");
        Label subtitle = new Label("Consulta composizione, sinterizzazione e documenti associati a un lotto.");
        subtitle.getStyleClass().add("registers-page-subtitle");

        VBox page = new VBox(22, new VBox(6, title, subtitle), searchCard, statusBox, emptyState, resultsContent);
        page.setMaxWidth(CONTENT_MAX_WIDTH);
        StackPane centered = new StackPane(page);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(30, 28, 40, 28));
        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("registers-scroll");

        BorderPane body = new BorderPane(scroll);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(header, body);
    }

    private void configureSearchField(ComboBox<String> comboBox, String prompt) {
        comboBox.setEditable(true);
        comboBox.setPromptText(prompt);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getStyleClass().add("registers-selector");
    }

    private VBox createField(String text, ComboBox<String> field) {
        Label label = new Label(text);
        label.getStyleClass().add("registers-field-label");
        VBox box = new VBox(6, label, field);
        box.setMinWidth(210);
        box.setPrefWidth(310);
        return box;
    }

    private Label invisibleLabel() {
        Label label = new Label(" ");
        label.setVisible(false);
        return label;
    }

    private VBox createIdentityCard() {
        identityItem.getStyleClass().add("registers-identity-value");
        identityLot.getStyleClass().add("registers-identity-value");
        identityComposition.getStyleClass().add("registers-badge");
        identityFiring.getStyleClass().add("registers-badge");
        Label found = new Label("● Registro trovato");
        found.getStyleClass().add("registers-found");
        HBox identity = new HBox(36,
                identityField("Articolo", identityItem), identityField("Lotto", identityLot));
        HBox badges = new HBox(10, found, identityComposition, identityFiring);
        VBox card = new VBox(14, identity, badges);
        card.getStyleClass().add("registers-identity-card");
        return card;
    }

    private VBox identityField(String title, Label value) {
        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("registers-identity-label");
        return new VBox(3, label, value);
    }

    private VBox createEmptyState() {
        Label icon = new Label("⌕");
        icon.getStyleClass().add("registers-empty-icon");
        Label title = new Label("Cerca un registro");
        title.getStyleClass().add("registers-empty-title");
        Label description = new Label("Seleziona un articolo e un lotto per visualizzare la relativa tracciabilità.");
        description.getStyleClass().add("registers-muted");
        description.setWrapText(true);
        VBox box = new VBox(8, icon, title, description);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("registers-empty-state");
        return box;
    }

    private Tab createTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public AppHeader getHeader() { return header; }
    public ComboBox<String> getArticleComboBox() { return articleComboBox; }
    public ComboBox<String> getLotComboBox() { return lotComboBox; }
    public Button getSearchButton() { return searchButton; }
    public Button getClearButton() { return clearButton; }
    public TabPane getHistoryTabs() { return historyTabs; }
    public Button getBuildCompositionDocumentButton() { return buildCompositionDocumentButton; }
    public Button getBuildFiringDocumentButton() { return buildFiringDocumentButton; }

    public void clearSearch() {
        articleComboBox.setValue(null);
        articleComboBox.getEditor().clear();
        lotComboBox.setValue(null);
        lotComboBox.getEditor().clear();
        hideResults();
        hideStatus();
    }

    public void showSearchLoading() {
        setPrimaryActionsDisabled(true);
        setProgressVisible(true);
        showStatus("Ricerca in corso…", false);
    }

    public void showSearchResult(RegistersSearchService.SearchResult result) {
        setPrimaryActionsDisabled(false);
        setProgressVisible(false);
        if (!result.success()) {
            hideResults();
            showStatus(result.message(), true);
            return;
        }

        RegistersSearchService.RegisterIdentity identity = result.identity();
        identityItem.setText(identity.itemCode());
        identityLot.setText(identity.lotCode());
        identityComposition.setText(identity.compositionVersion() == null
                ? "Composizione non disponibile" : "Composizione v." + identity.compositionVersion());
        identityFiring.setText("Firing #" + identity.firingId());
        compositionView.render(result.composition());
        firingView.render(result.firing());
        documentsView.render(result.documents());
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        resultsContent.setVisible(true);
        resultsContent.setManaged(true);
        showStatus("Registro caricato correttamente.", false);
    }

    public void showSearchError(String message) {
        setPrimaryActionsDisabled(false);
        setProgressVisible(false);
        hideResults();
        showStatus(message, true);
    }

    public void showSuggestionError() {
        if (!searchButton.isDisabled()) showStatus("Errore durante il caricamento dei suggerimenti.", true);
    }

    public void showDocumentGenerationLoading() {
        setPrimaryActionsDisabled(true);
        setProgressVisible(true);
        showStatus("Generazione documento in corso…", false);
    }

    public void showDocumentGenerationSuccess() {
        setPrimaryActionsDisabled(false);
        setProgressVisible(false);
        showStatus("Documento generato e aperto nel browser.", false);
    }

    public void showDocumentGenerationError(String message) {
        setPrimaryActionsDisabled(false);
        setProgressVisible(false);
        showStatus(message == null || message.isBlank() ? "Errore durante la generazione del documento." : message, true);
    }

    private void hideResults() {
        resultsContent.setVisible(false);
        resultsContent.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private void setPrimaryActionsDisabled(boolean disabled) {
        searchButton.setDisable(disabled);
        clearButton.setDisable(disabled);
        articleComboBox.setDisable(disabled);
        lotComboBox.setDisable(disabled);
        buildCompositionDocumentButton.setDisable(disabled);
    }

    private void setProgressVisible(boolean visible) {
        progressIndicator.setVisible(visible);
        progressIndicator.setManaged(visible);
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.getStyleClass().removeAll("registers-status-success", "registers-status-error");
        statusLabel.getStyleClass().add(error ? "registers-status-error" : "registers-status-success");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
