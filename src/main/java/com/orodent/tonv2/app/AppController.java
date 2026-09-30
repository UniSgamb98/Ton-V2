package com.orodent.tonv2.app;

import com.orodent.tonv2.app.navigation.CubageNavigator;
import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.cubage.creation.controller.CubageCreationController;
import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.service.CubageFormulaSetPersistenceService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import com.orodent.tonv2.features.cubage.home.controller.CubageController;
import com.orodent.tonv2.features.cubage.home.service.CubageService;
import com.orodent.tonv2.features.cubage.home.view.CubageView;
import com.orodent.tonv2.features.documents.archive.controller.DocumentsArchiveController;
import com.orodent.tonv2.features.documents.archive.view.DocumentsArchiveView;
import com.orodent.tonv2.features.documents.home.controller.DocumentsController;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;
import com.orodent.tonv2.features.documents.template.controller.TemplateEditorController;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import com.orodent.tonv2.features.inventory.controller.InventoryController;
import com.orodent.tonv2.features.inventory.view.InventoryView;
import com.orodent.tonv2.features.laboratory.composition.controller.CreateCompositionController;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.CreateDiskModelController;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.firingprogram.controller.FiringProgramController;
import com.orodent.tonv2.features.laboratory.firingprogram.service.FiringProgramService;
import com.orodent.tonv2.features.laboratory.firingprogram.view.FiringProgramView;
import com.orodent.tonv2.features.laboratory.home.controller.LaboratoryController;
import com.orodent.tonv2.features.laboratory.itemsetup.controller.ItemSetupController;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;
import com.orodent.tonv2.features.laboratory.production.controller.BatchProductionController;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionReadService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;
import com.orodent.tonv2.features.laboratory.presintering.controller.PresinteringController;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringDocumentParamsService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringReadService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;
import com.orodent.tonv2.features.laboratory.composition.controller.CompositionArchiveController;
import com.orodent.tonv2.features.laboratory.composition.view.CompositionArchiveView;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.DiskModelArchiveController;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import com.orodent.tonv2.features.laboratory.diskmodel.view.DiskModelArchiveView;
import com.orodent.tonv2.features.laboratory.home.view.LaboratoryView;
import com.orodent.tonv2.features.registers.home.controller.RegistersController;
import com.orodent.tonv2.features.registers.home.service.RegistersDocumentService;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;

public class AppController implements DocumentsNavigator, LaboratoryNavigator, CubageNavigator {
    /*
    Qua salvo i modelli dell'applicazione e tutte le variabili che servono all'intera applicazione e non alle
    singole pagine.
     */
    private final Stage stage;
    private final AppContainer app;
    private final String cssPath;
    private Runnable activePageCleanup = () -> {};

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

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Home");
    }

    @Override
    public void showDocumentsCreate() {
        TemplateEditorView view = new TemplateEditorView();
        configureHeader(view.getHeader());

        new TemplateEditorController(
                view,
                buildTemplateWorkflowService()
        );

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Nuovo documento");
    }

    @Override
    public void showDocumentsArchive() {
        DocumentsArchiveView view = new DocumentsArchiveView();
        configureHeader(view.getHeader());
        DocumentsArchiveController controller = new DocumentsArchiveController(
                view,
                app.templateEditorService(),
                this,
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Archivio template");
        controller.loadInitialData();
    }

    @Override
    public void showDocumentsEditTemplate(int templateId) {
        TemplateEditorService.TemplateSnapshot templateSnapshot = app.templateEditorService().getTemplateById(templateId);
        if (templateSnapshot == null) {
            showDocumentsArchive();
            return;
        }

        TemplateEditorView view = new TemplateEditorView();
        configureHeader(view.getHeader());

        new TemplateEditorController(
                view,
                buildTemplateWorkflowService(),
                TemplateEditorController.EditorMode.edit(templateId, templateSnapshot.sqlQuery(), this)
        );

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Modifica template");
    }

    private TemplateEditorWorkflowService buildTemplateWorkflowService() {
        BatchProductionDocumentParamsService batchPresetService = new BatchProductionDocumentParamsService(
                app.compositionRepo(),
                app.blankModelRepo(),
                app.blankModelLayerRepo(),
                app.compositionLayerIngredientRepo(),
                app.powderRepo(),
                app.itemRepo(),
                app.lineRepo()
        );
        PresinteringDocumentParamsService presinteringPresetService = new PresinteringDocumentParamsService(
                app.itemRepo()
        );

        return new TemplateEditorWorkflowService(
                app.templateEditorService(),
                app.database::getConnection,
                batchPresetService,
                presinteringPresetService
        );
    }

    public void showInventory() {
        InventoryView view = new InventoryView();
        configureHeader(view.getHeader());
        new InventoryController(view, app.itemRepo(), app.depotRepo(), app.stockRepo(), app.lotRepo());

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Inventario");
    }

    public void showCubage() {
        CubageView view = new CubageView();
        configureHeader(view.getHeader());
        new CubageController(view, new CubageService(), this);

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Cubaggio");
    }

    @Override
    public void showCubageCreation() {
        CubageCreationView view = new CubageCreationView();
        configureHeader(view.getHeader());
        new CubageCreationController(
                view,
                new CubageCreationService(
                        app.payloadContractRepo(),
                        app.payloadContractFieldRepo()
                ),
                new CubageFormulaSetPersistenceService(app.database.getConnection())
        );

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Gestione Calcoli Cubaggio");
    }

    @Override
    public void showCubageProductFormulaAssignments() {
        showFeatureNotAvailableAlert();
    }

    @Override
    public void showCubagePayloadContracts() {
        showFeatureNotAvailableAlert();
    }

    private void showFeatureNotAvailableAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("TON");
        alert.setHeaderText(null);
        alert.setContentText("Funzionalità non disponibile");
        alert.showAndWait();
    }

    public void showRegisters() {
        RegistersView view = new RegistersView();
        configureHeader(view.getHeader());
        RegistersSearchService searchService = new RegistersSearchService(
                app.connectionProvider(),
                app.templateEditorService()
        );

        RegistersController controller = new RegistersController(
                view,
                searchService,
                new RegistersDocumentService(
                        app.connectionProvider(),
                        app.templateEditorService()
                ),
                app.documentBrowserService(),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Registri");
        controller.loadInitialData();
    }

    @Override
    public void showCreateComposition() {
        showCreateCompositionInternal(null);
    }

    @Override
    public void showCreateComposition(int productId) {
        showCreateCompositionInternal(productId);
    }

    private void showCreateCompositionInternal(Integer productId) {
        CreateCompositionView view = new CreateCompositionView();
        configureHeader(view.getHeader());
        CreateCompositionController controller = new CreateCompositionController(
                view,
                this,
                new CreateCompositionService(
                        app.powderRepo(),
                        app.compositionRepo(),
                        app.compositionLayerIngredientRepo(),
                        app.productRepo(),
                        app.lineRepo(),
                        app.blankModelRepo()
                ),
                productId == null
                        ? CreateCompositionController.EditorMode.create()
                        : CreateCompositionController.EditorMode.edit()
        );

        if (productId != null) {
            CompositionArchiveService archiveService = new CompositionArchiveService(app.connectionProvider());
            archiveService.loadCompositionSnapshot(productId)
                    .ifPresent(controller::preloadFromArchiveSnapshot);
            controller.markAsClean();
        }

        showScene(createSceneWithCSS(view));
        stage.setTitle(productId == null ? "TON - Nuova composizione" : "TON - Modifica Composizione");
    }


    @Override
    public void showDocuments() {
        DocumentsView view = new DocumentsView();
        configureHeader(view.getHeader());
        new DocumentsController(view, this);

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Documentazione");
    }

    @Override
    public void showLaboratory() {
        LaboratoryView view = new LaboratoryView();
        configureHeader(view.getHeader());
        new LaboratoryController(view, this);

        showScene(createSceneWithCSS(view));
        stage.setTitle("TON - Laboratorio");
    }

    @Override
    public void showBatchProduction() {
        BatchProductionView view = new BatchProductionView();
        configureHeader(view.getHeader());
        BatchProductionController controller = new BatchProductionController(
                view,
                new BatchProductionService(
                        app.connectionProvider(),
                        app.templateEditorService()
                ),
                new BatchProductionReadService(
                        app.connectionProvider(),
                        app.templateEditorService()
                ),
                app.documentBrowserService(),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Produzione batch");
        controller.loadInitialData();
    }

    @Override
    public void showItemSetup() {
        ItemSetupView view = new ItemSetupView();
        configureHeader(view.getHeader());
        ItemSetupController controller = new ItemSetupController(
                view,
                new ItemSetupService(app.connectionProvider()),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Setup Item");
        controller.loadInitialData();
    }


    @Override
    public void showCreateFiringProgram() {
        FiringProgramView view = new FiringProgramView();
        configureHeader(view.getHeader());

        FiringProgramController controller = new FiringProgramController(
                view,
                new FiringProgramService(app.connectionProvider()),
                this,
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Nuovo Ciclo Sinterizzazione");
    }

    @Override
    public void showPresintering() {
        PresinteringView view = new PresinteringView();
        configureHeader(view.getHeader());
        PresinteringController controller = new PresinteringController(
                view,
                new PresinteringService(
                        app.connectionProvider(),
                        app.templateEditorService()
                ),
                new PresinteringReadService(
                        app.connectionProvider(),
                        app.templateEditorService()
                ),
                app.documentBrowserService(),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Presinterizza");
        controller.loadInitialData();
    }

    @Override
    public void showCreateDiskModel() {
        showCreateDiskModelInternal(null);
    }

    @Override
    public void showCreateDiskModel(int blankModelId) {
        showCreateDiskModelInternal(blankModelId);
    }

    private void showCreateDiskModelInternal(Integer blankModelId) {
        CreateDiskModelView view = new CreateDiskModelView();
        configureHeader(view.getHeader());
        CreateDiskModelController controller = new CreateDiskModelController(
                view,
                this,
                new CreateDiskModelService(app.connectionProvider()),
                new DiskModelArchiveService(app.connectionProvider()),
                blankModelId == null
                        ? CreateDiskModelController.EditorMode.create()
                        : CreateDiskModelController.EditorMode.edit(blankModelId),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle(blankModelId == null ? "TON - Nuovo modello disco" : "TON - Modifica Modello Disco");
        controller.loadInitialData();
    }

    @Override
    public void showLaboratoryCompositionArchive() {
        CompositionArchiveView view = new CompositionArchiveView();
        configureHeader(view.getHeader());
        CompositionArchiveController controller = new CompositionArchiveController(
                view,
                this,
                new CompositionArchiveService(app.connectionProvider()),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Archivio composizioni");
        controller.loadInitialData();
    }

    @Override
    public void showLaboratoryDiskModelArchive() {
        DiskModelArchiveView view = new DiskModelArchiveView();
        configureHeader(view.getHeader());
        DiskModelArchiveController controller = new DiskModelArchiveController(
                view,
                this,
                new DiskModelArchiveService(app.connectionProvider()),
                app.backgroundExecutor()
        );

        showScene(createSceneWithCSS(view), controller::dispose);
        stage.setTitle("TON - Archivio dischi");
        controller.loadInitialData();
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

    private void showScene(Scene scene) {
        showScene(scene, () -> {});
    }

    private void showScene(Scene scene, Runnable pageCleanup) {
        runActivePageCleanup();
        activePageCleanup = pageCleanup;
        stage.setScene(scene);
    }

    private void runActivePageCleanup() {
        try {
            activePageCleanup.run();
        } catch (RuntimeException exception) {
            System.err.println("Errore durante la chiusura della pagina corrente: " + exception.getMessage());
        }
    }

    private void configureHeader(AppHeader header) {
        header.getHomeButton().setOnAction(e -> showHome());
        header.getInventoryButton().setOnAction( e -> showInventory());
        header.getLaboratoryButton().setOnAction(e -> showLaboratory());
        header.getCubageButton().setOnAction(e -> showCubage());
        header.getDocumentsButton().setOnAction(e -> showDocuments());
        header.getRegistersButton().setOnAction(e -> showRegisters());
    }

    public void shutdown() {
        runActivePageCleanup();
        activePageCleanup = () -> {};
        app.shutdown();
        app.database.stop();
    }
}
