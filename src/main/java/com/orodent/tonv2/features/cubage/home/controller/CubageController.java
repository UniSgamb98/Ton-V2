package com.orodent.tonv2.features.cubage.home.controller;

import com.orodent.tonv2.features.cubage.home.service.CubageService;
import com.orodent.tonv2.features.cubage.home.view.CubageView;

public class CubageController {

    public CubageController(CubageView view, CubageService service) {
        view.setIntroText(service.getIntroMessage());
    }
}
