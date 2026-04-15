package com.orodent.tonv2.features.cubage.home.controller;

import com.orodent.tonv2.app.navigation.CubageNavigator;
import com.orodent.tonv2.features.cubage.home.service.CubageService;
import com.orodent.tonv2.features.cubage.home.view.CubageView;

public class CubageController {

    public CubageController(CubageView view, CubageService service, CubageNavigator navigator) {
        view.setIntroText(service.getIntroMessage());
        view.getCalculationManagementButton().setOnAction(e -> navigator.showCubageCalculationManagement());
        view.getProductAssignmentButton().setOnAction(e -> navigator.showCubageProductFormulaAssignments());
        view.getPayloadContractButton().setOnAction(e -> navigator.showCubagePayloadContracts());
    }
}
