package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.util.ArrayList;
import java.util.List;

public class BatchProductionController {

    private final BatchProductionView view;
    private final BatchProductionService service;
    private final DocumentBrowserService documentBrowserService;

    public BatchProductionController(BatchProductionView view,
                                     BatchProductionService service,
                                     DocumentBrowserService documentBrowserService) {
        this.view = view;
        this.service = service;
        this.documentBrowserService = documentBrowserService;

        setupActions();
    }

    private void setupActions() {
        view.setLines(service.findAllLines());

        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.setProductSelectionHandler(this::onProductSelected);
        view.getProduceButton().setOnAction(e -> produceBatch());
        refreshTemplateSelector();
    }

    private void onLineChanged() {
        Line selected = view.getLineSelector().getValue();
        if (selected == null) {
            view.clearProducts();
            view.setItemRows(List.of());
            return;
        }

        Product lineProduct = service.findProductById(selected.productId());
        view.setSelectableProducts(lineProduct == null ? List.of() : List.of(lineProduct), null);
        view.setItemRows(List.of());
        view.setFeedback("", false);
    }

    private void onProductSelected(Product product) {
        view.setItemRows(service.findItemsByProduct(product.id()));
        view.setFeedback("", false);
    }

    private void produceBatch() {
        try {
            Line line = view.getLineSelector().getValue();
            List<BatchProductionService.ProductionRequestLine> requestLines = collectLines();

            BatchProductionService.BatchResult result = service.produce(
                    line,
                    requestLines,
                    view.getNotesArea().getText()
            );

            String documentPath = service.generateDocumentIfTemplateSelected(
                    view.getTemplateSelector().getValue(),
                    line,
                    view.getNotesArea().getText(),
                    result.plan()
            );

            if (documentPath != null) {
                documentBrowserService.openDocument(documentPath);
            }

            view.setFeedback(
                    "Batch salvato. Ordine #" + result.persistResult().productionOrderId() +
                            " con " + result.plan().lines().size() + " righe, quantità totale " + result.persistResult().totalQuantity() + "." +
                            (documentPath == null ? "" : " Documento generato e aperto nel browser: " + documentPath),
                    false
            );
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante il salvataggio batch.", true);
        }
    }

    private List<BatchProductionService.ProductionRequestLine> collectLines() {
        List<BatchProductionService.ProductionRequestLine> lines = new ArrayList<>();

        for (BatchProductionView.BatchRow row : view.getRows()) {
            Item item = row.getItem();
            String qtyRaw = row.getQuantityField().getText();

            int qty;
            if (qtyRaw == null || qtyRaw.isBlank()) {
                qty = 0;
            } else {
                try {
                    qty = Integer.parseInt(qtyRaw.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Quantità non valida per l'item " + item.code() + ".");
                }
            }

            lines.add(new BatchProductionService.ProductionRequestLine(item.id(), qty));
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Seleziona un prodotto con item disponibili prima di produrre.");
        }

        return lines;
    }

    private void refreshTemplateSelector() {
        view.setTemplateNames(service.findTemplateNames(), service.getLastTemplateName());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                service.setLastTemplateName(newValue)
        );
    }
}
