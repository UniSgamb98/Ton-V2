package com.orodent.tonv2.features.registers.home.controller;

import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;

public class RegistersController {
    private final RegistersView view;
    private final RegistersSearchService searchService;

    public RegistersController(RegistersView view, RegistersSearchService searchService) {
        this.view = view;
        this.searchService = searchService;

        bindActions();
    }

    private void bindActions() {
        view.getSearchButton().setOnAction(e -> runSearch());
    }

    private void runSearch() {
        RegistersSearchService.SearchResult result = searchService.search(
                view.getArticleField().getText(),
                view.getLotField().getText()
        );

        view.getCompositionSummaryArea().setText(result.compositionOutput());
        view.getFiringSummaryArea().setText(result.firingOutput());
        view.getDocumentsPreviewArea().setText(result.documentsOutput());
    }

    public RegistersView getView() {
        return view;
    }
}
