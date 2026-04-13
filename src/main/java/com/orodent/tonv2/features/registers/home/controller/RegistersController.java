package com.orodent.tonv2.features.registers.home.controller;

import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.registers.home.service.RegistersDocumentService;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;

import java.util.List;

public class RegistersController {
    private static final int MAX_SUGGESTIONS = 30;

    private final RegistersView view;
    private final RegistersSearchService searchService;
    private final RegistersDocumentService documentService;
    private final DocumentBrowserService documentBrowserService;
    private final PauseTransition itemDebounce;
    private final PauseTransition lotDebounce;

    private boolean updatingSuggestions;

    public RegistersController(RegistersView view,
                               RegistersSearchService searchService,
                               RegistersDocumentService documentService,
                               DocumentBrowserService documentBrowserService) {
        this.view = view;
        this.searchService = searchService;
        this.documentService = documentService;
        this.documentBrowserService = documentBrowserService;
        this.itemDebounce = new PauseTransition(Duration.millis(250));
        this.lotDebounce = new PauseTransition(Duration.millis(250));

        bindActions();
        bootstrapSuggestions();
    }

    private void bindActions() {
        view.getSearchButton().setOnAction(e -> runSearch());

        itemDebounce.setOnFinished(e -> refreshItemSuggestions());
        lotDebounce.setOnFinished(e -> refreshLotSuggestions());

        view.getArticleComboBox().getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            itemDebounce.playFromStart();
        });

        view.getLotComboBox().getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            lotDebounce.playFromStart();
        });

        view.getArticleComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            lotDebounce.playFromStart();
        });

        view.getLotComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSuggestions) {
                return;
            }
            itemDebounce.playFromStart();
        });

        view.getBuildCompositionDocumentButton().setOnAction(e -> generateCompositionDocument());
    }

    private void bootstrapSuggestions() {
        refreshItemSuggestions();
        refreshLotSuggestions();
    }

    private void refreshItemSuggestions() {
        String lotInput = getEditorText(view.getLotComboBox());
        String itemInput = getEditorText(view.getArticleComboBox());

        List<String> suggestions;
        if (!lotInput.isBlank()) {
            suggestions = searchService.suggestItemCodesByLotPrefix(lotInput, MAX_SUGGESTIONS);
            if (!itemInput.isBlank()) {
                String upperItemInput = itemInput.toUpperCase();
                suggestions = suggestions.stream()
                        .filter(code -> code.toUpperCase().startsWith(upperItemInput))
                        .toList();
            }
        } else {
            suggestions = searchService.suggestItemCodesByPrefix(itemInput, MAX_SUGGESTIONS);
        }

        applySuggestions(view.getArticleComboBox(), suggestions, itemInput);
    }

    private void refreshLotSuggestions() {
        String itemInput = getEditorText(view.getArticleComboBox());
        String lotInput = getEditorText(view.getLotComboBox());

        List<String> suggestions;
        if (!itemInput.isBlank()) {
            suggestions = searchService.suggestLotCodesByItemCode(itemInput, lotInput, MAX_SUGGESTIONS);
        } else {
            suggestions = searchService.suggestLotCodesByPrefix(lotInput, MAX_SUGGESTIONS);
        }

        applySuggestions(view.getLotComboBox(), suggestions, lotInput);
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
        RegistersSearchService.SearchResult result = searchService.search(
                getEditorText(view.getArticleComboBox()),
                getEditorText(view.getLotComboBox())
        );

        view.getCompositionSummaryArea().setText(result.compositionOutput());
        view.getFiringSummaryArea().setText(result.firingOutput());
        view.getDocumentsPreviewArea().setText(result.documentsOutput());
    }

    public RegistersView getView() {
        return view;
    }
}
