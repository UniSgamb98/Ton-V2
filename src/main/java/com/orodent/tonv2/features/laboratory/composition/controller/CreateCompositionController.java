package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.form.ConfirmUnsavedChangesDialog;
import com.orodent.tonv2.core.ui.form.DirtyStateTracker;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionDraftStateService;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CreateCompositionController {

    private static final Product NEW_PRODUCT_OPTION = new Product(-1, "➕ Nuovo prodotto", "");
    private static final String NEW_LINE_OPTION = "➕ Nuova linea";

    private final CreateCompositionView view;
    private final LaboratoryNavigator navigator;
    private final CreateCompositionService service;
    private final EditorMode editorMode;
    private final CompositionDraftStateService draftStateService;
    private final DirtyStateTracker dirtyStateTracker;

    public CreateCompositionController(CreateCompositionView view,
                                       LaboratoryNavigator navigator,
                                       CreateCompositionService service) {
        this(view, navigator, service, EditorMode.create());
    }

    public CreateCompositionController(CreateCompositionView view,
                                       LaboratoryNavigator navigator,
                                       CreateCompositionService service,
                                       EditorMode editorMode) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.editorMode = editorMode;
        this.draftStateService = new CompositionDraftStateService();
        this.dirtyStateTracker = new DirtyStateTracker()
                .track("productId", () -> view.getProductSelector().getValue() == null ? null : view.getProductSelector().getValue().id())
                .track("lineName", () -> normalize(view.getLineSelector().getValue()))
                .track("blankModelId", () -> view.getBlankModelSelector().getValue() == null ? null : view.getBlankModelSelector().getValue().id())
                .track("notes", () -> normalize(view.getNotes()))
                .track("layers", () -> draftStateService.buildLayersSignature(view.getLayers()));

        view.configureEditMode(editorMode.editMode());
        view.setLineSelectorLocked(editorMode.editMode());
        loadProducts();
        loadLines();
        loadBlankModels();
        loadPowders();
        setupActions();
        dirtyStateTracker.captureInitialState();
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
        view.getSaveButton().setOnAction(e -> saveComposition(true));
        view.getBackButton().setOnAction(e -> {
            if (editorMode.editMode()) {
                navigateBackWithConfirmation();
            }
        });
        view.getProductSelector().valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean isNewProduct = isNewProductOption(newValue);
            view.setLoadLatestVersionVisible(newValue != null && !isNewProduct);
            view.setLineSelectorLocked(editorMode.editMode() && !isNewProduct);
            applyLineSelectionForProduct(newValue, oldValue);
        });
        view.getBlankModelSelector().valueProperty().addListener((obs, oldModel, newModel) -> {
            if (newModel != null) {
                view.setLayerCount(newModel.numLayers());
            }
        });
        view.getLoadLatestVersionButton().setOnAction(e -> loadLatestVersion());
        view.setLoadLatestVersionVisible(false);
    }

    private void applyLineSelectionForProduct(Product newProduct, Product oldProduct) {
        if (newProduct == null) {
            return;
        }

        if (isNewProductOption(newProduct)) {
            if (editorMode.editMode()) {
                view.getLineSelector().setValue(NEW_LINE_OPTION);
            }
            return;
        }

        if (oldProduct != null && oldProduct.id() == newProduct.id()) {
            return;
        }

        List<String> lines = service.findLineNamesByProductId(newProduct.id());
        if (!lines.isEmpty()) {
            view.getLineSelector().setValue(lines.get(0));
        }
    }

    private void navigateBackWithConfirmation() {
        if (!dirtyStateTracker.hasUnsavedChanges()) {
            navigator.showLaboratoryCompositionArchive();
            return;
        }

        ConfirmUnsavedChangesDialog.UserChoice choice = ConfirmUnsavedChangesDialog.show(
                "Modifiche non salvate",
                "Vuoi salvare le modifiche prima di tornare all'archivio?",
                "Se scegli 'Non salvare' perderai le modifiche effettuate.",
                "Salva e torna"
        );

        if (choice == ConfirmUnsavedChangesDialog.UserChoice.SAVE) {
            if (saveComposition(false)) {
                navigator.showLaboratoryCompositionArchive();
            }
            return;
        }

        if (choice == ConfirmUnsavedChangesDialog.UserChoice.DISCARD) {
            navigator.showLaboratoryCompositionArchive();
        }
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

    public void preloadFromArchiveSnapshot(CompositionArchiveService.CompositionSnapshot snapshot) {
        view.getProductSelector().getItems().stream()
                .filter(product -> product.id() == snapshot.productId())
                .findFirst()
                .ifPresent(view.getProductSelector()::setValue);

        if (snapshot.blankModelId() != null) {
            view.getBlankModelSelector().getItems().stream()
                    .filter(model -> model.id() == snapshot.blankModelId())
                    .findFirst()
                    .ifPresent(view.getBlankModelSelector()::setValue);
        }

        if (snapshot.lineName() != null && !snapshot.lineName().isBlank()) {
            view.getLineSelector().setValue(snapshot.lineName());
        }

        view.setNotes(snapshot.notes());
        view.replaceLayers(snapshot.layerDrafts());
    }

    public void markAsClean() {
        dirtyStateTracker.captureInitialState();
    }

    private boolean saveComposition(boolean navigateToLaboratory) {
        ProductSelection productSelection = resolveProductSelection();
        if (productSelection == null) {
            return false;
        }

        String lineName = resolveLineName();
        if (lineName == null) {
            return false;
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

            dirtyStateTracker.captureInitialState();

            if (navigateToLaboratory) {
                navigator.showLaboratory();
            }
            return true;
        } catch (IllegalArgumentException ex) {
            showWarning("Validazione dati", ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            showDbError("Errore salvataggio composizione", ex);
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value;
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

    public record EditorMode(boolean editMode) {
        public static EditorMode create() {
            return new EditorMode(false);
        }

        public static EditorMode edit() {
            return new EditorMode(true);
        }
    }
}
