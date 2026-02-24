package com.orodent.tonv2.core.database.model;

public record BlankModel(
        int id,
        String code,
        double superiorOvermaterialMm,
        double inferiorOvermaterialMm,
        double pressureKgCm2,
        double gramsPerMm
) {
    @Override
    public String toString() {
        return code;
    }
}

