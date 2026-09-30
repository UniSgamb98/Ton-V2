package com.orodent.tonv2.features.laboratory.firingprogram.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.FiringProgramRepositoryImpl;
import com.orodent.tonv2.core.database.model.FiringProgram;
import com.orodent.tonv2.core.database.repository.FiringProgramRepository;

import java.util.List;

public class FiringProgramService {

    private final ConnectionProvider connectionProvider;

    public FiringProgramService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public FiringProgram saveProgram(String programName, List<StepInput> steps) {
        String normalizedName = programName == null ? "" : programName.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Inserisci il nome del programma.");
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Aggiungi almeno uno step al programma.");
        }

        validateSteps(steps);

        return connectionProvider.withTransaction(connection -> {
            FiringProgramRepository repository = new FiringProgramRepositoryImpl(connection);
            FiringProgram program = repository.insertProgram(normalizedName);
            for (int i = 0; i < steps.size(); i++) {
                StepInput step = steps.get(i);
                repository.insertStep(
                        program.id(),
                        i + 1,
                        step.targetTemperature(),
                        step.rampTimeMinutes(),
                        step.holdTimeMinutes()
                );
            }
            return program;
        });
    }

    private void validateSteps(List<StepInput> steps) {
        for (int i = 0; i < steps.size(); i++) {
            StepInput step = steps.get(i);
            int index = i + 1;
            if (step.targetTemperature() <= 0) {
                throw new IllegalArgumentException("Step " + index + ": temperatura di arrivo non valida.");
            }
            if (step.rampTimeMinutes() < 0) {
                throw new IllegalArgumentException("Step " + index + ": tempo di rampa non valido.");
            }
            if (step.holdTimeMinutes() < 0) {
                throw new IllegalArgumentException("Step " + index + ": tempo di mantenuta non valido.");
            }
        }
    }

    public record StepInput(double targetTemperature, int rampTimeMinutes, int holdTimeMinutes) {
    }
}
