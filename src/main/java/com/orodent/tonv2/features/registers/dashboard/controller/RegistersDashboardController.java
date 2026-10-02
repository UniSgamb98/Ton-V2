package com.orodent.tonv2.features.registers.dashboard.controller;

import com.orodent.tonv2.app.navigation.RegistersNavigator;
import com.orodent.tonv2.features.registers.dashboard.view.RegistersDashboardView;

public final class RegistersDashboardController {
    public RegistersDashboardController(RegistersDashboardView view, RegistersNavigator navigator) {
        view.getProducedDisksArchiveButton().setOnAction(event -> navigator.showRegistersArchive());
        view.getSalesUpdateButton().setOnAction(event -> navigator.showSalesUpdate());
    }
}
