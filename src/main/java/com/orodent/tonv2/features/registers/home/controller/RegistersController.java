package com.orodent.tonv2.features.registers.home.controller;

import com.orodent.tonv2.features.registers.home.view.RegistersView;

public class RegistersController {
    private final RegistersView view;

    public RegistersController(RegistersView view) {
        this.view = view;
    }

    public RegistersView getView() {
        return view;
    }
}
