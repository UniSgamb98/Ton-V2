package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.FiringProgram;
import com.orodent.tonv2.core.database.model.FiringProgramStep;

import java.util.List;

public interface FiringProgramRepository {
    FiringProgram insertProgram(String name);
    void insertStep(int firingProgramId, int stepOrder, double targetTemperature, int rampTimeMinutes, int holdTimeMinutes);
    List<FiringProgramStep> findStepsByProgramId(int firingProgramId);
}
