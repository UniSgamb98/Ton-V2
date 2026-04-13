package com.orodent.tonv2.features.laboratory.firingprogram.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.form.FieldParsers;
import com.orodent.tonv2.features.laboratory.firingprogram.service.FiringProgramService;
import com.orodent.tonv2.features.laboratory.firingprogram.view.FiringProgramView;

import java.util.ArrayList;
import java.util.List;

public class FiringProgramController {

    private final FiringProgramView view;
    private final FiringProgramService service;
    private final LaboratoryNavigator navigator;

    public FiringProgramController(FiringProgramView view,
                                   FiringProgramService service,
                                   LaboratoryNavigator navigator) {
        this.view = view;
        this.service = service;
        this.navigator = navigator;

        bindActions();
        addStep();
    }

    private void bindActions() {
        view.getBackButton().setOnAction(e -> navigator.showLaboratory());
        view.getAddStepButton().setOnAction(e -> addStep());
        view.getSaveButton().setOnAction(e -> saveProgram());
    }

    private void addStep() {
        FiringProgramView.StepRow row = view.addStepRow();
        row.getRemoveButton().setOnAction(e -> {
            view.removeStepRow(row);
            view.refreshStepLabels();
        });
        view.refreshStepLabels();
    }

    private void saveProgram() {
        try {
            List<FiringProgramService.StepInput> steps = collectSteps();
            var program = service.saveProgram(view.getProgramNameField().getText(), steps);
            view.getFeedbackLabel().setText("Programma salvato con successo (#" + program.id() + ").");
        } catch (IllegalArgumentException ex) {
            view.getFeedbackLabel().setText(ex.getMessage());
        }
    }

    private List<FiringProgramService.StepInput> collectSteps() {
        List<FiringProgramService.StepInput> steps = new ArrayList<>();
        int index = 1;
        for (FiringProgramView.StepRow row : view.getStepRows()) {
            double targetTemp = parseRequiredDouble(row.getTargetTemperatureField().getText(), index, "temperatura di arrivo");
            int ramp = parseRequiredInt(row.getRampTimeField().getText(), index, "tempo di rampa");
            int hold = parseRequiredInt(row.getHoldTimeField().getText(), index, "tempo di mantenuta");
            steps.add(new FiringProgramService.StepInput(targetTemp, ramp, hold));
            index++;
        }
        return steps;
    }

    private int parseRequiredInt(String value, int stepIndex, String fieldLabel) {
        String fieldName = "Step " + stepIndex + " - " + fieldLabel;
        Integer parsed = FieldParsers.parseInteger(value, fieldName);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " è obbligatorio.");
        }
        return parsed;
    }

    private double parseRequiredDouble(String value, int stepIndex, String fieldLabel) {
        String fieldName = "Step " + stepIndex + " - " + fieldLabel;
        Double parsed = FieldParsers.parseDouble(value, fieldName);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " è obbligatorio.");
        }
        return parsed;
    }

    public FiringProgramView getView() {
        return view;
    }
}
