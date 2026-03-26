package com.orodent.tonv2.app;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.documents.home.controller.DocumentsController;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;
import com.orodent.tonv2.features.documents.template.controller.TemplateEditorController;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import com.orodent.tonv2.features.inventory.controller.InventoryController;
import com.orodent.tonv2.features.inventory.view.InventoryView;
import com.orodent.tonv2.features.laboratory.composition.controller.CreateCompositionController;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.CreateDiskModelController;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.home.controller.LaboratoryController;
import com.orodent.tonv2.features.laboratory.itemsetup.controller.ItemSetupController;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;
import com.orodent.tonv2.features.laboratory.production.controller.BatchProductionController;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;
import com.orodent.tonv2.features.laboratory.presintering.controller.PresinteringController;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import com.orodent.tonv2.features.laboratory.home.view.LaboratoryView;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class AppController {
    /*
    Qua salvo i modelli dell'applicazione e tutte le variabili che servono all'intera applicazione e non alle
    singole pagine.
     */
    private final Stage stage;
    private final AppContainer app;
    private final String cssPath;
    private final TemplateEditorService templateEditorService;

    /*
    In questo progetto l'applicazione è state-less. Che significa che tutte le View vengono create da zero sempre.
    Questo significa che i riferimenti dei controller qui sotto non sono necessari. Sarebbero tornati utili nel caso:
    - i controller mantenessero uno stato per ricordare dei filtri impostati dall'utente, il testo inserito in
    un form oppure una selezione di qualche genere.
    - il controller gestisce qualcosa di continuo come Thread, Timer, connessioni TCP. Insomma tutto ciò che ha bisogno
    di essere fermato in un secondo momento oppure se sta leggendo un flusso di dati.
     */

    public AppController(Stage stage) {
        this.stage = stage;
        this.app = new AppContainer();
        this.cssPath = Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm();
        this.templateEditorService = new TemplateEditorService(app.database::getConnection);

        showHome();
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    /*
    -------------------------------------------------------------------------------------------------------------------
    Creo un metodo per ogni view che devo mostrare. Ogni metodo chiama configureHeader per assegnare le funzioni dei
    tasti dello header qua e non nei singoli controller di tutte le view.
     */

    public void showHome() {
        HomeView view = new HomeView();
        configureHeader(view.getHeader());

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Home");
    }

    public void showDocumentsCreate() {
        TemplateEditorView view = new TemplateEditorView();
        configureHeader(view.getHeader());
        BatchProductionDocumentParamsService batchPresetService = new BatchProductionDocumentParamsService(
                app.compositionRepo(),
                app.blankModelRepo(),
                app.blankModelLayerRepo(),
                app.compositionLayerIngredientRepo(),
                app.powderRepo(),
                app.itemRepo(),
                app.lineRepo()
        );
        new TemplateEditorController(view, templateEditorService, app.database::getConnection, batchPresetService);

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Nuovo documento");
    }

    public void showDocumentsArchive() {
        showDocuments();
    }

    public void showDocumentsSearch() {
        showDocuments();
    }

    public void showInventory() {
        InventoryView view = new InventoryView();
        configureHeader(view.getHeader());
        new InventoryController(view, app.itemRepo(), app.depotRepo(), app.stockRepo(), app.lotRepo());

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Inventario");
    }

    public void showCreateComposition() {
        CreateCompositionView view = new CreateCompositionView();
        configureHeader(view.getHeader());
        new CreateCompositionController(
                view,
                this,
                new CreateCompositionService(
                        app.powderRepo(),
                        app.compositionRepo(),
                        app.compositionLayerIngredientRepo(),
                        app.productRepo(),
                        app.blankModelRepo()
                )
        );

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Nuova composizione");
    }


    public void showDocuments() {
        DocumentsView view = new DocumentsView();
        configureHeader(view.getHeader());
        new DocumentsController(view, this);

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Documentazione");
    }


    public void showLaboratory() {
        LaboratoryView view = new LaboratoryView();
        configureHeader(view.getHeader());
        new LaboratoryController(view, this);

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Laboratorio");
    }


    public void showBatchProduction() {
        showBatchProduction(java.util.List.of());
    }

    public void showBatchProduction(java.util.List<com.orodent.tonv2.core.database.model.Item> preselectedItems) {
        BatchProductionView view = new BatchProductionView();
        configureHeader(view.getHeader());
        BatchProductionDocumentParamsService batchDocumentParamsService = new BatchProductionDocumentParamsService(
                app.compositionRepo(),
                app.blankModelRepo(),
                app.blankModelLayerRepo(),
                app.compositionLayerIngredientRepo(),
                app.powderRepo(),
                app.itemRepo(),
                app.lineRepo()
        );
        new BatchProductionController(
                view,
                app.itemRepo(),
                app.lineRepo(),
                app.compositionRepo(),
                app.productRepo(),
                app.productionRepo(),
                new BatchProductionService(),
                templateEditorService,
                batchDocumentParamsService,
                preselectedItems
        );

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Produzione batch");
    }

    public void showItemSetup() {
        ItemSetupView view = new ItemSetupView();
        configureHeader(view.getHeader());
        new ItemSetupController(
                view,
                new ItemSetupService(app.itemRepo(), app.compositionRepo(), app.productRepo())
        );

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Setup Item");
    }

    public void showPresintering() {
        PresinteringView view = new PresinteringView();
        configureHeader(view.getHeader());
        new PresinteringController(
                view,
                new PresinteringService(app.productionRepo())
        );

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Presinterizza");
    }

    public void showCreateDiskModel() {
        CreateDiskModelView view = new CreateDiskModelView();
        configureHeader(view.getHeader());
        new CreateDiskModelController(
                view,
                this,
                new CreateDiskModelService(app.blankModelRepo(), app.blankModelLayerRepo(), app.blankModelHeightOvermaterialRepo())
        );

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Nuovo modello disco");
    }

    /*
    Creando le Scenes con questo metodo vengono collegate al CSS che può essere scritto tutto in un unico file.
     */
    private Scene createSceneWithCSS(Object root, String... extraCss) {
        Scene scene = new Scene((javafx.scene.Parent) root, 900, 700);
        scene.getStylesheets().add(cssPath);

        for (String css : extraCss) {
            String path = Objects.requireNonNull(
                    getClass().getResource(css)
            ).toExternalForm();

            scene.getStylesheets().add(path);
        }
        return scene;
    }

    private void configureHeader(AppHeader header) {
        header.getHomeButton().setOnAction(e -> showHome());
        header.getInventoryButton().setOnAction( e -> showInventory());
        header.getLaboratoryButton().setOnAction(e -> showLaboratory());
        header.getDocumentsButton().setOnAction(e -> showDocuments());
    }

    public void shutdown() {
        app.database.stop();
    }
}
