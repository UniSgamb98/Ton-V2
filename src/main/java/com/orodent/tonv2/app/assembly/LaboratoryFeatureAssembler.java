package com.orodent.tonv2.app.assembly;

import com.orodent.tonv2.app.AppPage;
import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.features.documents.browser.service.DocumentBrowserService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.laboratory.composition.controller.CompositionArchiveController;
import com.orodent.tonv2.features.laboratory.composition.controller.CreateCompositionController;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.service.CreateCompositionService;
import com.orodent.tonv2.features.laboratory.composition.view.CompositionArchiveView;
import com.orodent.tonv2.features.laboratory.composition.view.CreateCompositionView;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.CreateDiskModelController;
import com.orodent.tonv2.features.laboratory.diskmodel.controller.DiskModelArchiveController;
import com.orodent.tonv2.features.laboratory.diskmodel.service.CreateDiskModelService;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;
import com.orodent.tonv2.features.laboratory.diskmodel.view.DiskModelArchiveView;
import com.orodent.tonv2.features.laboratory.firingprogram.controller.FiringProgramController;
import com.orodent.tonv2.features.laboratory.firingprogram.service.FiringProgramService;
import com.orodent.tonv2.features.laboratory.firingprogram.view.FiringProgramView;
import com.orodent.tonv2.features.laboratory.home.controller.LaboratoryController;
import com.orodent.tonv2.features.laboratory.home.view.LaboratoryView;
import com.orodent.tonv2.features.laboratory.itemsetup.controller.ItemSetupController;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import com.orodent.tonv2.features.laboratory.itemsetup.view.ItemSetupView;
import com.orodent.tonv2.features.laboratory.presintering.controller.PresinteringController;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringReadService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;
import com.orodent.tonv2.features.laboratory.production.controller.BatchProductionController;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionReadService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.util.concurrent.Executor;

public final class LaboratoryFeatureAssembler {
    private final ConnectionProvider connectionProvider;
    private final TemplateEditorService templateEditorService;
    private final DocumentBrowserService documentBrowserService;
    private final Executor executor;

    public LaboratoryFeatureAssembler(ConnectionProvider connectionProvider,
                                      TemplateEditorService templateEditorService,
                                      DocumentBrowserService documentBrowserService,
                                      Executor executor) {
        this.connectionProvider = connectionProvider;
        this.templateEditorService = templateEditorService;
        this.documentBrowserService = documentBrowserService;
        this.executor = executor;
    }

    public AppPage home(LaboratoryNavigator navigator) {
        LaboratoryView view = new LaboratoryView();
        new LaboratoryController(view, navigator);
        return AppPage.immediate(view, view.getHeader(), "TON - Laboratorio");
    }

    public AppPage composition(Integer productId, LaboratoryNavigator navigator) {
        CreateCompositionView view = new CreateCompositionView();
        CreateCompositionController controller = new CreateCompositionController(
                view,
                navigator,
                new CreateCompositionService(connectionProvider),
                new CompositionArchiveService(connectionProvider),
                productId == null
                        ? CreateCompositionController.EditorMode.create()
                        : CreateCompositionController.EditorMode.edit(),
                productId,
                executor
        );
        String title = productId == null ? "TON - Nuova composizione" : "TON - Modifica Composizione";
        return new AppPage(view, view.getHeader(), title, controller::loadInitialData, controller::dispose);
    }

    public AppPage batchProduction() {
        BatchProductionView view = new BatchProductionView();
        BatchProductionController controller = new BatchProductionController(
                view,
                new BatchProductionService(connectionProvider, templateEditorService),
                new BatchProductionReadService(connectionProvider, templateEditorService),
                documentBrowserService,
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Produzione batch",
                controller::loadInitialData, controller::dispose);
    }

    public AppPage itemSetup() {
        ItemSetupView view = new ItemSetupView();
        ItemSetupController controller = new ItemSetupController(
                view,
                new ItemSetupService(connectionProvider),
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Setup Item",
                controller::loadInitialData, controller::dispose);
    }

    public AppPage firingProgram(LaboratoryNavigator navigator) {
        FiringProgramView view = new FiringProgramView();
        FiringProgramController controller = new FiringProgramController(
                view,
                new FiringProgramService(connectionProvider),
                navigator,
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Nuovo Ciclo Sinterizzazione", null, controller::dispose);
    }

    public AppPage presintering() {
        PresinteringView view = new PresinteringView();
        PresinteringController controller = new PresinteringController(
                view,
                new PresinteringService(connectionProvider, templateEditorService),
                new PresinteringReadService(connectionProvider, templateEditorService),
                documentBrowserService,
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Presinterizza",
                controller::loadInitialData, controller::dispose);
    }

    public AppPage diskModel(Integer blankModelId, LaboratoryNavigator navigator) {
        CreateDiskModelView view = new CreateDiskModelView();
        CreateDiskModelController controller = new CreateDiskModelController(
                view,
                navigator,
                new CreateDiskModelService(connectionProvider),
                new DiskModelArchiveService(connectionProvider),
                blankModelId == null
                        ? CreateDiskModelController.EditorMode.create()
                        : CreateDiskModelController.EditorMode.edit(blankModelId),
                executor
        );
        String title = blankModelId == null ? "TON - Nuovo modello disco" : "TON - Modifica Modello Disco";
        return new AppPage(view, view.getHeader(), title, controller::loadInitialData, controller::dispose);
    }

    public AppPage compositionArchive(LaboratoryNavigator navigator) {
        CompositionArchiveView view = new CompositionArchiveView();
        CompositionArchiveController controller = new CompositionArchiveController(
                view,
                navigator,
                new CompositionArchiveService(connectionProvider),
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Archivio composizioni",
                controller::loadInitialData, controller::dispose);
    }

    public AppPage diskModelArchive(LaboratoryNavigator navigator) {
        DiskModelArchiveView view = new DiskModelArchiveView();
        DiskModelArchiveController controller = new DiskModelArchiveController(
                view,
                navigator,
                new DiskModelArchiveService(connectionProvider),
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Archivio dischi",
                controller::loadInitialData, controller::dispose);
    }
}
