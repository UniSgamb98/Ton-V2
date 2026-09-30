package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionReadService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class BatchProductionController {

    private final BatchProductionView view;
    private final BatchProductionService service;
    private final BatchProductionReadService readService;
    private final DocumentBrowserService documentBrowserService;
    private final DebouncedTaskRunner<BatchProductionReadService.InitialData> initialDataLoader;
    private final DebouncedTaskRunner<List<Product>> productsLoader;
    private final DebouncedTaskRunner<List<Item>> itemsLoader;

    public BatchProductionController(BatchProductionView view,
                                     BatchProductionService service,
                                     BatchProductionReadService readService,
                                     DocumentBrowserService documentBrowserService,
                                     Executor backgroundExecutor) {
        this.view = view;
        this.service = service;
        this.readService = readService;
        this.documentBrowserService = documentBrowserService;
        this.initialDataLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.productsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.itemsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);

        setupActions();
    }

    private void setupActions() {
        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.setProductSelectionHandler(this::onProductSelected);
        view.getProduceButton().setOnAction(e -> produceBatch());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                service.setLastTemplateName(newValue)
        );
    }

    public void loadInitialData() {
        initialDataLoader.runNow(
                readService::loadInitialData,
                view::showInitialLoading,
                data -> {
                    view.setLines(data.lines());
                    view.setTemplateNames(data.templateNames(), data.selectedTemplateName());
                    view.showLoadSuccess();
                },
                error -> view.showLoadError("Errore durante il caricamento della produzione batch.")
        );
    }

    private void onLineChanged() {
        Line selected = view.getLineSelector().getValue();
        if (selected == null) {
            productsLoader.cancel();
            itemsLoader.cancel();
            view.clearProducts();
            view.setItemRows(List.of());
            return;
        }

        itemsLoader.cancel();
        view.clearProducts();
        view.setItemRows(List.of());
        productsLoader.runNow(
                () -> readService.findProductsByLineName(selected.name()),
                view::showProductsLoading,
                products -> {
                    view.setSelectableProducts(products, null);
                    view.showLoadSuccess();
                },
                error -> view.showLoadError("Errore durante il caricamento dei prodotti.")
        );
    }

    private void onProductSelected(Product product) {
        view.setItemRows(List.of());
        itemsLoader.runNow(
                () -> readService.findItemsByProduct(product.id()),
                view::showItemsLoading,
                items -> {
                    view.setItemRows(items);
                    view.showLoadSuccess();
                },
                error -> view.showLoadError("Errore durante il caricamento degli item.")
        );
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

    public void dispose() {
        initialDataLoader.cancel();
        productsLoader.cancel();
        itemsLoader.cancel();
    }
}
