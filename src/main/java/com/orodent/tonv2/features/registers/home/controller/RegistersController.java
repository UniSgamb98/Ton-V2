package com.orodent.tonv2.features.registers.home.controller;

import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.registers.home.service.RegistersDocumentService;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class RegistersController {
    private static final int MAX_SUGGESTIONS = 30;

    private final RegistersView view;
    private final RegistersSearchService searchService;
    private final RegistersDocumentService documentService;
    private final DocumentBrowserService documentBrowserService;
    private final DebouncedTaskRunner<List<String>> itemSuggestionsLoader;
    private final DebouncedTaskRunner<List<String>> lotSuggestionsLoader;
    private final DebouncedTaskRunner<RegistersSearchService.SearchResult> searchLoader;

    private boolean updatingSuggestions;

    public RegistersController(RegistersView view,
                               RegistersSearchService searchService,
                               RegistersDocumentService documentService,
                               DocumentBrowserService documentBrowserService,
                               Executor backgroundExecutor) {
        this.view = view;
        this.searchService = searchService;
        this.documentService = documentService;
        this.documentBrowserService = documentBrowserService;
        this.itemSuggestionsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(250));
        this.lotSuggestionsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(250));
        this.searchLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);

        bindActions();
    }

    private void bindActions() {
        view.getSearchButton().setOnAction(e -> runSearch());

        view.getArticleComboBox().getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            loadItemSuggestions(true);
        });

        view.getLotComboBox().getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            loadLotSuggestions(true);
        });

        view.getArticleComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            loadLotSuggestions(true);
        });

        view.getLotComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            loadItemSuggestions(true);
        });

        view.getBuildCompositionDocumentButton().setOnAction(e -> generateCompositionDocument());
    }

    public void loadInitialData() {
        loadItemSuggestions(false);
        loadLotSuggestions(false);
    }

    private void loadItemSuggestions(boolean debounced) {
        String lotInput = getEditorText(view.getLotComboBox());
        String itemInput = getEditorText(view.getArticleComboBox());
        Callable<List<String>> operation = () -> findItemSuggestions(itemInput, lotInput);
        Consumer<List<String>> success = suggestions ->
                applySuggestions(view.getArticleComboBox(), suggestions, itemInput);

        if (debounced) {
            itemSuggestionsLoader.runDebounced(operation, () -> {}, success, error -> view.showSuggestionError());
        } else {
            itemSuggestionsLoader.runNow(operation, () -> {}, success, error -> view.showSuggestionError());
        }
    }

    private List<String> findItemSuggestions(String itemInput, String lotInput) {
        List<String> suggestions = lotInput.isBlank()
                ? searchService.suggestItemCodesByPrefix(itemInput, MAX_SUGGESTIONS)
                : searchService.suggestItemCodesByLotPrefix(lotInput, MAX_SUGGESTIONS);

        if (lotInput.isBlank() || itemInput.isBlank()) {
            return suggestions;
        }

        String upperItemInput = itemInput.toUpperCase();
        return suggestions.stream()
                .filter(code -> code.toUpperCase().startsWith(upperItemInput))
                .toList();
    }

    private void loadLotSuggestions(boolean debounced) {
        String itemInput = getEditorText(view.getArticleComboBox());
        String lotInput = getEditorText(view.getLotComboBox());
        Callable<List<String>> operation = () -> itemInput.isBlank()
                ? searchService.suggestLotCodesByPrefix(lotInput, MAX_SUGGESTIONS)
                : searchService.suggestLotCodesByItemCode(itemInput, lotInput, MAX_SUGGESTIONS);
        Consumer<List<String>> success = suggestions ->
                applySuggestions(view.getLotComboBox(), suggestions, lotInput);

        if (debounced) {
            lotSuggestionsLoader.runDebounced(operation, () -> {}, success, error -> view.showSuggestionError());
        } else {
            lotSuggestionsLoader.runNow(operation, () -> {}, success, error -> view.showSuggestionError());
        }
    }

    private void applySuggestions(ComboBox<String> comboBox, List<String> suggestions, String typedValue) {
        String safeTypedValue = typedValue == null ? "" : typedValue;

        updatingSuggestions = true;
        comboBox.setItems(FXCollections.observableArrayList(suggestions));

        if (suggestions.size() == 1) {
            String match = suggestions.getFirst();
            if (!match.equals(comboBox.getValue())) {
                comboBox.setValue(match);
            }
            comboBox.getEditor().setText(match);
            comboBox.getEditor().positionCaret(match.length());
        } else {
            comboBox.getEditor().setText(safeTypedValue);
            comboBox.getEditor().positionCaret(safeTypedValue.length());
        }

        comboBox.hide();
        if (!suggestions.isEmpty()) {
            comboBox.show();
        }
        updatingSuggestions = false;
    }

    private String getEditorText(ComboBox<String> comboBox) {
        String text = comboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }


    private void generateCompositionDocument() {
        try {
            String documentPath = documentService.generateCompositionDocument(
                    getEditorText(view.getArticleComboBox()),
                    getEditorText(view.getLotComboBox())
            );
            documentBrowserService.openDocument(documentPath);
        } catch (IllegalArgumentException ex) {
            view.getCompositionSummaryArea().setText(ex.getMessage());
        }
    }

    private void runSearch() {
        String itemCode = getEditorText(view.getArticleComboBox());
        String lotCode = getEditorText(view.getLotComboBox());
        itemSuggestionsLoader.cancel();
        lotSuggestionsLoader.cancel();
        searchLoader.runNow(
                () -> searchService.search(itemCode, lotCode),
                view::showSearchLoading,
                view::showSearchResult,
                error -> view.showSearchError("Errore durante la ricerca nei registri.")
        );
    }

    public void dispose() {
        itemSuggestionsLoader.cancel();
        lotSuggestionsLoader.cancel();
        searchLoader.cancel();
        documentService.close();
    }

    public RegistersView getView() {
        return view;
    }
}
