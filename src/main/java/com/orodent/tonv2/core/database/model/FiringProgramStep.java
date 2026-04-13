package com.orodent.tonv2.core.database.model;

public record FiringProgramStep(int id,
                                int firingProgramId,
                                int stepOrder,
                                double targetTemperature,
                                int rampTimeMinutes,
                                int holdTimeMinutes) {
}
