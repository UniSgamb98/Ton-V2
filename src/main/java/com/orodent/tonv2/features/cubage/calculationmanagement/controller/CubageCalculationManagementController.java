package com.orodent.tonv2.features.cubage.calculationmanagement.controller;

import com.orodent.tonv2.features.cubage.calculationmanagement.service.CubageCalculationManagementService;
import com.orodent.tonv2.features.cubage.calculationmanagement.view.CubageCalculationManagementView;

public class CubageCalculationManagementController {

    public CubageCalculationManagementController(CubageCalculationManagementView view,
                                                 CubageCalculationManagementService service) {
        view.setInfoText(service.getIntroMessage());
    }
}
