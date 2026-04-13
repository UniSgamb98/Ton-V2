package com.orodent.tonv2.features.laboratory.firingprogram.service;

import com.orodent.tonv2.core.database.model.FiringProgram;
import com.orodent.tonv2.core.database.repository.FiringProgramRepository;

import java.sql.Connection;
import java.util.List;

public class FiringProgramService {

    private final Connection connection;
    private final FiringProgramRepository repository;

    public FiringProgramService(Connection connection, FiringProgramRepository repository) {
        this.connection = connection;
        this.repository = repository;
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

        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
        } catch (Exception e) {
            throw new IllegalArgumentException("Errore durante il salvataggio del ciclo di sinterizzazione.");
        }

        try {
            connection.setAutoCommit(false);

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

            connection.commit();
            return program;
        } catch (IllegalArgumentException e) {
            rollbackQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new IllegalArgumentException("Errore durante il salvataggio del ciclo di sinterizzazione.");
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (Exception ignored) {
                // no-op
            }
        }
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

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (Exception ignored) {
            // no-op
        }
    }

    public record StepInput(double targetTemperature, int rampTimeMinutes, int holdTimeMinutes) {
    }
}
