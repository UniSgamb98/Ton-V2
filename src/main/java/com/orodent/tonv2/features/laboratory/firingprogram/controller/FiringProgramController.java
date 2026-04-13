package com.orodent.tonv2.features.laboratory.firingprogram.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
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
            double targetTemp = parseDouble(row.getTargetTemperatureField().getText(), "Step " + index + ": temperatura di arrivo non valida.");
            int ramp = parseInt(row.getRampTimeField().getText(), "Step " + index + ": tempo di rampa non valido.");
            int hold = parseInt(row.getHoldTimeField().getText(), "Step " + index + ": tempo di mantenuta non valido.");
            steps.add(new FiringProgramService.StepInput(targetTemp, ramp, hold));
            index++;
        }
        return steps;
    }

    private int parseInt(String value, String errorMessage) {
        try {
            return Integer.parseInt(normalize(value));
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(normalize(value).replace(',', '.'));
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Campo obbligatorio.");
        }
        return normalized;
    }

    public FiringProgramView getView() {
        return view;
    }
}
