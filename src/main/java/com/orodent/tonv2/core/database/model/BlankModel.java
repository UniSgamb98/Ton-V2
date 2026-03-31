package com.orodent.tonv2.core.database.model;

public record BlankModel(
        int id,
        String code,
        int version,
        double diameterMm,
        double superiorOvermaterialDefaultMm,
        double inferiorOvermaterialDefaultMm,
        double pressureKgCm2,
        double gramsPerMm,
        int numLayers
) {
    @Override
    public String toString() {
        return code + " (v" + version + ")";
    }
}
