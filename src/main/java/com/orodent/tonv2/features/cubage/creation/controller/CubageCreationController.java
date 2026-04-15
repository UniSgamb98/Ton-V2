package com.orodent.tonv2.features.cubage.creation.controller;

import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import com.orodent.tonv2.features.cubage.creation.view.CubageCreationView;

public class CubageCreationController {

    public CubageCreationController(CubageCreationView view,
                                    CubageCreationService service) {
        view.setInfoText(service.getIntroMessage());
    }
}
