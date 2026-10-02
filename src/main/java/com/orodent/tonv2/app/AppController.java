package com.orodent.tonv2.app;

import com.orodent.tonv2.app.navigation.CubageNavigator;
import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.app.navigation.RegistersNavigator;
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
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import com.orodent.tonv2.features.inventory.importing.controller.InventorySalesUpdateController;
import com.orodent.tonv2.features.inventory.importing.presentation.InventoryImportPresenter;
import com.orodent.tonv2.features.inventory.importing.service.InventorySnapshotImportService;
import com.orodent.tonv2.features.inventory.importing.view.InventorySalesUpdateView;
import com.orodent.tonv2.features.laboratory.composition.controller.CreateCompositionController;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.CreateDiskModelController;
import com.orodent.tonv2.features.laboratory.diskmodel.presentation.DiskModelEditorState;
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
import com.orodent.tonv2.features.registers.dashboard.controller.RegistersDashboardController;
import com.orodent.tonv2.features.registers.dashboard.view.RegistersDashboardView;
import com.orodent.tonv2.features.registers.home.controller.RegistersController;
import com.orodent.tonv2.features.registers.home.service.RegistersDocumentService;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;

public class AppController implements DocumentsNavigator, LaboratoryNavigator, CubageNavigator, RegistersNavigator {
    private static final double INITIAL_SCENE_WIDTH = 900;
    private static final double INITIAL_SCENE_HEIGHT = 700;

    /*
    Qua salvo i modelli dell'applicazione e tutte le variabili che servono all'intera applicazione e non alle
    singole pagine.
     */
    private final Stage stage;
    private final AppContainer app;
    private final String cssPath;
    private Scene scene;
    private Runnable activePageCleanup = () -> {};

    /*
    In questo progetto l'applicazione è state-less. Che significa che tutte le View vengono create da zero sempre.
    Questo significa che i riferimenti dei controller qui sotto non sono necessari. Sarebbero tornati utili nel caso:
    - i controller mantenessero uno stato per ricordare dei filtri impostati dall'utente, il testo inserito in
    un form oppure una selezione di qualche genere.
    - il controller gestisce qualcosa di continuo come Thread, Timer, connessioni TCP. Insomma tutto ciò che ha bisogno
    di essere fermato in un secondo momento oppure se sta leggendo un flusso di dati.
     */

    public AppController(Stage stage, AppContainer app) {
        this.stage = stage;
        this.app = app;
        this.cssPath = Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm();

        showHome();
    }

    /*
    -------------------------------------------------------------------------------------------------------------------
    Creo un metodo per ogni view che devo mostrare. Ogni metodo chiama configureHeader per assegnare le funzioni dei
    tasti dello header qua e non nei singoli controller di tutte le view.
     */

    public void showHome() {
        HomeView view = new HomeView();
        configureHeader(view.getHeader());

        showView(view);
        stage.setTitle("TON - Home");
    }

    @Override
    public void showDocumentsCreate() {
        TemplateEditorView view = new TemplateEditorView();
        configureHeader(view.getHeader());

        TemplateEditorController controller = new TemplateEditorController(
                view,
                buildTemplateWorkflowService(),
                app.backgroundExecutor()
        );

        showView(view, controller::dispose);
        stage.setTitle("TON - Nuovo documento");
        controller.loadInitialData();
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

        showView(view, controller::dispose);
        stage.setTitle("TON - Archivio template");
        controller.loadInitialData();
    }

    @Override
    public void showDocumentsEditTemplate(int templateId) {
        TemplateEditorView view = new TemplateEditorView();
        configureHeader(view.getHeader());

        TemplateEditorController controller = new TemplateEditorController(
                view,
                buildTemplateWorkflowService(),
                TemplateEditorController.EditorMode.edit(templateId, this),
                app.backgroundExecutor()
        );

        showView(view, controller::dispose);
        stage.setTitle("TON - Modifica template");
        controller.loadInitialData();
    }

    private TemplateEditorWorkflowService buildTemplateWorkflowService() {
        BatchProductionDocumentParamsService batchPresetService =
                new BatchProductionDocumentParamsService(app.connectionProvider());
        PresinteringDocumentParamsService presinteringPresetService =
                new PresinteringDocumentParamsService(app.connectionProvider());

        return new TemplateEditorWorkflowService(
                app.templateEditorService(),
                app.connectionProvider(),
                batchPresetService,
                presinteringPresetService
        );
    }

    public void showCubage() {
        CubageView view = new CubageView();
        configureHeader(view.getHeader());
        new CubageController(view, new CubageService(), this);

        showView(view, "/css/features/feature-dashboard.css");
        stage.setTitle("TON - Cubaggio");
    }

    @Override
    public void showCubageCreation() {
        CubageCreationView view = new CubageCreationView();
        configureHeader(view.getHeader());
        CubageCreationController controller = new CubageCreationController(
                view,
                new CubageCreationService(app.connectionProvider()),
                new CubageFormulaSetPersistenceService(app.connectionProvider()),
                app.backgroundExecutor()
        );

        showView(view, controller::dispose);
        stage.setTitle("TON - Gestione Calcoli Cubaggio");
        controller.loadInitialData();
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

    @Override
    public void showRegisters() {
        RegistersDashboardView view = new RegistersDashboardView();
        configureHeader(view.getHeader());
        new RegistersDashboardController(view, this);

        showView(view, "/css/features/feature-dashboard.css");
        stage.setTitle("TON - Registri");
    }

    @Override
    public void showRegistersArchive() {
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

        showView(view, controller::dispose, "/css/features/registers.css");
        stage.setTitle("TON - Archivio dischi prodotti");
        controller.loadInitialData();
    }

    @Override
    public void showSalesUpdate() {
        InventorySalesUpdateView view = new InventorySalesUpdateView();
        configureHeader(view.getHeader());
        InventorySalesUpdateController controller = new InventorySalesUpdateController(
                view,
                new InventorySnapshotImportService(),
                new InventoryImportPresenter(),
                app.backgroundExecutor()
        );

        showView(view, controller::dispose, "/css/features/sales-import.css");
        stage.setTitle("TON - Aggiorna vendite");
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
                new CreateCompositionService(app.connectionProvider()),
                new CompositionArchiveService(app.connectionProvider()),
                productId == null
                        ? CreateCompositionController.EditorMode.create()
                        : CreateCompositionController.EditorMode.edit(),
                productId,
                app.backgroundExecutor()
        );

        showView(view, controller::dispose, "/css/features/composition-editor.css");
        stage.setTitle(productId == null ? "TON - Nuova composizione" : "TON - Modifica Composizione");
        controller.loadInitialData();
    }


    @Override
    public void showDocuments() {
        DocumentsView view = new DocumentsView();
        configureHeader(view.getHeader());
        new DocumentsController(view, this);

        showView(view, "/css/features/feature-dashboard.css");
        stage.setTitle("TON - Documentazione");
    }

    @Override
    public void showLaboratory() {
        LaboratoryView view = new LaboratoryView();
        configureHeader(view.getHeader());
        new LaboratoryController(view, this);

        showView(view, "/css/features/laboratory.css");
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

        showView(view, controller::dispose, "/css/features/batch-production.css");
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

        showView(view, controller::dispose, "/css/item-setup.css");
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

        showView(view, controller::dispose);
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

        showView(view, controller::dispose);
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
        CreateDiskModelView view = new CreateDiskModelView(new DiskModelEditorState());
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

        showView(view, controller::dispose, "/css/features/disk-model-editor.css");
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

        showView(view, controller::dispose);
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

        showView(view, controller::dispose);
        stage.setTitle("TON - Archivio dischi");
        controller.loadInitialData();
    }

    /*
    La Scene viene creata soltanto all'apertura dell'applicazione. Durante la navigazione viene sostituito il root,
    così lo Stage conserva dimensioni e stato della finestra. Ogni pagina può aggiungere i propri fogli di stile.
     */
    private void showView(Parent root, String... extraCss) {
        showView(root, () -> {}, extraCss);
    }

    private void showView(Parent root, Runnable pageCleanup, String... extraCss) {
        runActivePageCleanup();
        activePageCleanup = pageCleanup;

        if (scene == null) {
            scene = new Scene(root, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        scene.getStylesheets().setAll(cssPath);
        for (String css : extraCss) {
            String path = Objects.requireNonNull(
                    getClass().getResource(css)
            ).toExternalForm();

            scene.getStylesheets().add(path);
        }
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
        header.getLaboratoryButton().setOnAction(e -> showLaboratory());
        header.getCubageButton().setOnAction(e -> showCubage());
        header.getDocumentsButton().setOnAction(e -> showDocuments());
        header.getRegistersButton().setOnAction(e -> showRegisters());
    }

    public void shutdown() {
        runActivePageCleanup();
        activePageCleanup = () -> {};
        app.shutdown();
    }
}
