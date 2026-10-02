package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.production.presentation.BatchProductionFormState;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionReadService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;
import javafx.util.Duration;

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
    private final DebouncedTaskRunner<ProductionCompletion> productionLoader;
    private final BatchProductionFormState formState = new BatchProductionFormState();

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
        this.productionLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);

        setupActions();
    }

    private void setupActions() {
        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.setProductSelectionHandler(this::onProductSelected);
        view.setQuantityChangeHandler((item, quantity) -> {
            formState.setQuantity(item, quantity);
            renderState();
        });
        view.getClearQuantitiesButton().setOnAction(e -> {
            formState.clearQuantities();
            view.clearQuantities();
            renderState();
        });
        view.getProduceButton().setOnAction(e -> produceBatch());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) -> {
            formState.setTemplateName(newValue);
            service.setLastTemplateName(newValue);
            renderState();
        });
        renderState();
    }

    public void loadInitialData() {
        initialDataLoader.runNow(
                readService::loadInitialData,
                () -> startLoading(view::showInitialLoading),
                data -> {
                    view.setLines(data.lines());
                    view.setTemplateNames(data.templateNames(), data.selectedTemplateName());
                    finishLoading();
                },
                error -> showError("Errore durante il caricamento della produzione batch.")
        );
    }

    private void onLineChanged() {
        Line selected = view.getLineSelector().getValue();
        formState.selectLine(selected);
        renderState();
        if (selected == null) {
            productsLoader.cancel();
            itemsLoader.cancel();
            view.clearProducts();
            view.setItemRows(List.of());
            formState.setItems(List.of());
            renderState();
            return;
        }

        itemsLoader.cancel();
        view.clearProducts();
        view.setItemRows(List.of());
        productsLoader.runNow(
                () -> readService.findProductsByLineName(selected.name()),
                () -> startLoading(view::showProductsLoading),
                products -> {
                    view.setSelectableProducts(products, null);
                    finishLoading();
                },
                error -> showError("Errore durante il caricamento dei prodotti.")
        );
    }

    private void onProductSelected(Product product) {
        formState.selectProduct(product);
        view.setItemRows(List.of());
        renderState();
        itemsLoader.runNow(
                () -> readService.findItemsByProduct(product.id()),
                () -> startLoading(view::showItemsLoading),
                items -> {
                    formState.setItems(items);
                    view.setItemRows(items);
                    finishLoading();
                },
                error -> showError("Errore durante il caricamento degli articoli.")
        );
    }

    private void produceBatch() {
        try {
            Line line = view.getLineSelector().getValue();
            List<BatchProductionService.ProductionRequestLine> requestLines = collectLines();
            String notes = view.getNotesArea().getText();
            String templateName = view.getTemplateSelector().getValue();

            productionLoader.runNow(
                    () -> produceAndGenerateDocument(line, requestLines, notes, templateName),
                    () -> startLoading(view::showProductionSaving),
                    completion -> {
                        finishLoading();
                        if (completion.documentPath() != null) {
                            documentBrowserService.openDocument(completion.documentPath());
                        }
                        BatchProductionService.BatchResult result = completion.result();
                        String suffix = completion.documentError() == null
                                ? (completion.documentPath() == null ? "" : " Documento generato e aperto nel browser: " + completion.documentPath())
                                : " Produzione salvata, ma il documento non è stato generato: " + completion.documentError();
                        view.setFeedback(
                                "Batch salvato. Ordine #" + result.persistResult().productionOrderId() +
                                        " con " + result.plan().lines().size() + " righe, quantità totale " +
                                        result.persistResult().totalQuantity() + "." + suffix,
                                completion.documentError() != null
                        );
                    },
                    error -> showError(error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Errore durante il salvataggio batch.")
            );
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        }
    }

    private ProductionCompletion produceAndGenerateDocument(
            Line line,
            List<BatchProductionService.ProductionRequestLine> requestLines,
            String notes,
            String templateName) {
        BatchProductionService.BatchResult result = service.produce(line, requestLines, notes);
        try {
            String documentPath = service.generateDocumentIfTemplateSelected(
                    templateName, line, notes, result.plan()
            );
            return new ProductionCompletion(result, documentPath, null);
        } catch (RuntimeException exception) {
            return new ProductionCompletion(result, null, exception.getMessage());
        }
    }

    private List<BatchProductionService.ProductionRequestLine> collectLines() {
        List<BatchProductionService.ProductionRequestLine> lines = formState.itemQuantities().stream()
                .map(entry -> new BatchProductionService.ProductionRequestLine(
                        entry.item().id(), entry.quantity()))
                .toList();

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Seleziona un prodotto con item disponibili prima di produrre.");
        }

        return lines;
    }

    private void renderState() {
        view.render(formState.toViewState());
    }

    private void startLoading(Runnable viewLoadingAction) {
        formState.setLoading(true);
        renderState();
        viewLoadingAction.run();
    }

    private void finishLoading() {
        formState.setLoading(false);
        view.showLoadSuccess();
        renderState();
    }

    private void showError(String message) {
        formState.setLoading(false);
        view.showLoadError(message);
        renderState();
    }

    public void dispose() {
        initialDataLoader.cancel();
        productsLoader.cancel();
        itemsLoader.cancel();
        productionLoader.cancel();
    }

    private record ProductionCompletion(BatchProductionService.BatchResult result,
                                        String documentPath,
                                        String documentError) {
    }
}
