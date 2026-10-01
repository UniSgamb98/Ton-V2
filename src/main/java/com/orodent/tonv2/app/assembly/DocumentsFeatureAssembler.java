package com.orodent.tonv2.app.assembly;

import com.orodent.tonv2.app.AppPage;
import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.features.documents.archive.controller.DocumentsArchiveController;
import com.orodent.tonv2.features.documents.archive.view.DocumentsArchiveView;
import com.orodent.tonv2.features.documents.home.controller.DocumentsController;
import com.orodent.tonv2.features.documents.home.view.DocumentsView;
import com.orodent.tonv2.features.documents.template.controller.TemplateEditorController;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorWorkflowService;
import com.orodent.tonv2.features.documents.template.view.TemplateEditorView;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;

import java.util.concurrent.Executor;

public final class DocumentsFeatureAssembler {
    private final TemplateEditorService templateEditorService;
    private final ConnectionProvider connectionProvider;
    private final Executor executor;

    public DocumentsFeatureAssembler(TemplateEditorService templateEditorService,
                                     ConnectionProvider connectionProvider,
                                     Executor executor) {
        this.templateEditorService = templateEditorService;
        this.connectionProvider = connectionProvider;
        this.executor = executor;
    }

    public AppPage home(DocumentsNavigator navigator) {
        DocumentsView view = new DocumentsView();
        new DocumentsController(view, navigator);
        return AppPage.immediate(view, view.getHeader(), "TON - Documentazione");
    }

    public AppPage createTemplate() {
        return templateEditor(TemplateEditorController.EditorMode.create(), "TON - Nuovo documento");
    }

    public AppPage editTemplate(int templateId, DocumentsNavigator navigator) {
        return templateEditor(
                TemplateEditorController.EditorMode.edit(templateId, navigator),
                "TON - Modifica template"
        );
    }

    public AppPage archive(DocumentsNavigator navigator) {
        DocumentsArchiveView view = new DocumentsArchiveView();
        DocumentsArchiveController controller = new DocumentsArchiveController(
                view,
                templateEditorService,
                navigator,
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Archivio template",
                controller::loadInitialData, controller::dispose);
    }

    private AppPage templateEditor(TemplateEditorController.EditorMode mode, String title) {
        TemplateEditorView view = new TemplateEditorView();
        TemplateEditorController controller = new TemplateEditorController(
                view,
                buildWorkflowService(),
                mode,
                executor
        );
        return new AppPage(view, view.getHeader(), title, controller::loadInitialData, controller::dispose);
    }

    private TemplateEditorWorkflowService buildWorkflowService() {
        return new TemplateEditorWorkflowService(
                templateEditorService,
                connectionProvider,
                new BatchProductionDocumentParamsService(connectionProvider),
                new PresinteringDocumentParamsService(connectionProvider)
        );
    }
}
