package com.orodent.tonv2.app.assembly;

import com.orodent.tonv2.app.AppPage;
import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.features.documents.browser.service.DocumentBrowserService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.registers.home.controller.RegistersController;
import com.orodent.tonv2.features.registers.home.service.RegistersDocumentService;
import com.orodent.tonv2.features.registers.home.service.RegistersSearchService;
import com.orodent.tonv2.features.registers.home.view.RegistersView;

import java.util.concurrent.Executor;

public final class RegistersFeatureAssembler {
    private final ConnectionProvider connectionProvider;
    private final TemplateEditorService templateEditorService;
    private final DocumentBrowserService documentBrowserService;
    private final Executor executor;

    public RegistersFeatureAssembler(ConnectionProvider connectionProvider,
                                     TemplateEditorService templateEditorService,
                                     DocumentBrowserService documentBrowserService,
                                     Executor executor) {
        this.connectionProvider = connectionProvider;
        this.templateEditorService = templateEditorService;
        this.documentBrowserService = documentBrowserService;
        this.executor = executor;
    }

    public AppPage home() {
        RegistersView view = new RegistersView();
        RegistersController controller = new RegistersController(
                view,
                new RegistersSearchService(connectionProvider, templateEditorService),
                new RegistersDocumentService(connectionProvider, templateEditorService),
                documentBrowserService,
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Registri",
                controller::loadInitialData, controller::dispose);
    }
}
