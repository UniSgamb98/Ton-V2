package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.sql.SQLException;
import java.util.Optional;

public class CreateCompositionController {

    private static final Product NEW_PRODUCT_OPTION = new Product(-1, "➕ Nuovo prodotto", "");
    private static final String NEW_LINE_OPTION = "➕ Nuova linea";

    private final CreateCompositionView view;
    private final LaboratoryNavigator navigator;
    private final CreateCompositionService service;

    public CreateCompositionController(CreateCompositionView view,
                                       LaboratoryNavigator navigator,
                                       CreateCompositionService service) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;

        loadProducts();
        loadLines();
        loadBlankModels();
        loadPowders();
        setupActions();
    }

    private void loadProducts() {
        view.getProductSelector().getItems().setAll(service.findAllProducts());
        if (!view.getProductSelector().getItems().contains(NEW_PRODUCT_OPTION)) {
            view.getProductSelector().getItems().addFirst(NEW_PRODUCT_OPTION);
        }
    }

    private void loadPowders() {
        view.setAvailablePowders(service.findAllPowders());
    }

    private void loadLines() {
        view.getLineSelector().getItems().setAll(service.findAllLineNames());
        if (!view.getLineSelector().getItems().contains(NEW_LINE_OPTION)) {
            view.getLineSelector().getItems().addFirst(NEW_LINE_OPTION);
        }
    }

    private void loadBlankModels() {
        view.getBlankModelSelector().getItems().setAll(service.findAllBlankModels());
    }

    private void setupActions() {
        view.getSaveButton().setOnAction(e -> saveComposition());
        view.getProductSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                view.setLoadLatestVersionVisible(newValue != null && !isNewProductOption(newValue))
        );
        view.getBlankModelSelector().valueProperty().addListener((obs, oldModel, newModel) -> {
            if (newModel != null) {
                view.setLayerCount(newModel.numLayers());
            }
        });
        view.getLoadLatestVersionButton().setOnAction(e -> loadLatestVersion());
        view.setLoadLatestVersionVisible(false);
    }

    private void loadLatestVersion() {
        Product selectedProduct = view.getProductSelector().getValue();

        if (selectedProduct == null || isNewProductOption(selectedProduct)) {
            return;
        }

        Optional<CreateCompositionService.LatestCompositionData> latest = service.loadLatestComposition(selectedProduct.id());
        if (latest.isEmpty()) {
            showWarning("Nessuna versione trovata", "Non esiste ancora una composizione da caricare per questo prodotto.");
            return;
        }

        CreateCompositionService.LatestCompositionData data = latest.get();
        if (data.blankModelId() != null) {
            view.getBlankModelSelector().getItems().stream()
                    .filter(model -> model.id() == data.blankModelId())
                    .findFirst()
                    .ifPresent(view.getBlankModelSelector()::setValue);
        }

        view.setNotes(data.notes());
        view.replaceLayers(data.layerDrafts());
    }


    public void preloadFromProductId(int productId) {
        view.getProductSelector().getItems().stream()
                .filter(product -> product.id() == productId)
                .findFirst()
                .ifPresent(product -> {
                    view.getProductSelector().setValue(product);
                    loadLatestVersion();
                });
    }

    private void saveComposition() {
        ProductSelection productSelection = resolveProductSelection();
        if (productSelection == null) {
            return;
        }

        String lineName = resolveLineName();
        if (lineName == null) {
            return;
        }

        BlankModel blankModel = view.getBlankModelSelector().getValue();
        view.renumberLayers();

        try {
            service.saveComposition(new CreateCompositionService.SaveCompositionRequest(
                    productSelection.product(),
                    productSelection.newProductCode(),
                    lineName,
                    blankModel,
                    view.getLayers(),
                    view.getNotes()
            ));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Composizione Salvata");
            alert.setContentText("La ricetta è stata registrata correttamente.");
            alert.showAndWait();

            navigator.showLaboratory();
        } catch (IllegalArgumentException ex) {
            showWarning("Validazione dati", ex.getMessage());
        } catch (RuntimeException ex) {
            showDbError("Errore salvataggio composizione", ex);
        }
    }

    private ProductSelection resolveProductSelection() {
        Product selected = view.getProductSelector().getValue();

        if (selected != null && !isNewProductOption(selected)) {
            return new ProductSelection(selected, null);
        }

        return showMissingProductDialog().orElse(null);
    }

    private boolean isNewProductOption(Product product) {
        return product != null && product.id() == NEW_PRODUCT_OPTION.id();
    }

    private String resolveLineName() {
        String selected = view.getLineSelector().getValue();
        if (selected == null || selected.isBlank()) {
            return showMissingLineDialog().orElse(null);
        }
        if (NEW_LINE_OPTION.equals(selected)) {
            return showMissingLineDialog().orElse(null);
        }
        return selected;
    }

    private Optional<ProductSelection> showMissingProductDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuovo prodotto");
        dialog.setHeaderText("Product selector vuoto");
        dialog.setContentText("Codice nuovo prodotto:");

        dialog.getEditor().setPromptText("Inserisci codice prodotto");

        Label helper = new Label(
                "Stai creando una composizione per un nuovo prodotto.\n" +
                        "Se vuoi creare una nuova versione di un prodotto esistente, " +
                        "premi Annulla, torna indietro e seleziona il prodotto nel Product selector."
        );
        helper.setWrapText(true);
        dialog.getDialogPane().setExpandableContent(helper);
        dialog.getDialogPane().setExpanded(true);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        try {
            String newProductCode = result.get() == null ? null : result.get().trim();
            if (newProductCode == null || newProductCode.isBlank()) {
                throw new IllegalArgumentException("Inserisci un codice prodotto valido per continuare.");
            }
            return Optional.of(new ProductSelection(null, newProductCode));
        } catch (IllegalArgumentException ex) {
            showWarning("Codice prodotto mancante", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> showMissingLineDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuova linea");
        dialog.setHeaderText("Line selector vuoto");
        dialog.setContentText("Nome nuova linea:");
        dialog.getEditor().setPromptText("Inserisci nome linea");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        String lineName = result.get() == null ? null : result.get().trim();
        if (lineName == null || lineName.isBlank()) {
            showWarning("Nome linea mancante", "Inserisci un nome linea valido per continuare.");
            return Optional.empty();
        }

        return Optional.of(lineName);
    }

    private void showWarning(String header, String content) {
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setHeaderText(header);
        warning.setContentText(content);
        warning.showAndWait();
    }

    private void showDbError(String header, RuntimeException ex) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }

        String message = ex.getMessage();
        if (cause instanceof SQLException sqlEx && "45000".equals(sqlEx.getSQLState())) {
            message = sqlEx.getMessage();
        }

        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setHeaderText(header);
        error.setContentText(message);
        error.showAndWait();
    }

    private record ProductSelection(Product product, String newProductCode) {}
}
