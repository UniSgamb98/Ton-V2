package com.orodent.tonv2.app.assembly;

import com.orodent.tonv2.app.AppPage;
import com.orodent.tonv2.app.navigation.CubageNavigator;
import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.features.cubage.creation.controller.CubageCreationController;
import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.service.CubageFormulaSetPersistenceService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;
import com.orodent.tonv2.features.cubage.home.controller.CubageController;
import com.orodent.tonv2.features.cubage.home.service.CubageService;
import com.orodent.tonv2.features.cubage.home.view.CubageView;

import java.util.concurrent.Executor;

public final class CubageFeatureAssembler {
    private final ConnectionProvider connectionProvider;
    private final Executor executor;

    public CubageFeatureAssembler(ConnectionProvider connectionProvider, Executor executor) {
        this.connectionProvider = connectionProvider;
        this.executor = executor;
    }

    public AppPage home(CubageNavigator navigator) {
        CubageView view = new CubageView();
        new CubageController(view, new CubageService(), navigator);
        return AppPage.immediate(view, view.getHeader(), "TON - Cubaggio");
    }

    public AppPage creation() {
        CubageCreationView view = new CubageCreationView();
        CubageCreationController controller = new CubageCreationController(
                view,
                new CubageCreationService(connectionProvider),
                new CubageFormulaSetPersistenceService(connectionProvider),
                executor
        );
        return new AppPage(view, view.getHeader(), "TON - Gestione Calcoli Cubaggio",
                controller::loadInitialData, controller::dispose);
    }
}
