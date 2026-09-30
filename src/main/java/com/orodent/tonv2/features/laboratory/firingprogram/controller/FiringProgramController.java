package com.orodent.tonv2.features.laboratory.firingprogram.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.form.FieldParsers;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.core.database.model.FiringProgram;
import com.orodent.tonv2.features.laboratory.firingprogram.service.FiringProgramService;
import com.orodent.tonv2.features.laboratory.firingprogram.view.FiringProgramView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javafx.util.Duration;

public class FiringProgramController {

    private final FiringProgramView view;
    private final FiringProgramService service;
    private final LaboratoryNavigator navigator;
    private final DebouncedTaskRunner<FiringProgram> saveRunner;

    public FiringProgramController(FiringProgramView view,
                                   FiringProgramService service,
                                   LaboratoryNavigator navigator,
                                   Executor backgroundExecutor) {
        this.view = view;
        this.service = service;
        this.navigator = navigator;
        this.saveRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);

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
            String programName = view.getProgramNameField().getText();
            saveRunner.runNow(
                    () -> service.saveProgram(programName, steps),
                    view::showSaving,
                    program -> view.showSaveSuccess("Programma salvato con successo (#" + program.id() + ")."),
                    error -> view.showSaveError(error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Errore durante il salvataggio del ciclo di sinterizzazione.")
            );
        } catch (IllegalArgumentException ex) {
            view.showSaveError(ex.getMessage());
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

    public void dispose() {
        saveRunner.cancel();
    }
}
