package com.orodent.tonv2.core.database.model;

public record PowderOxide(
        int id,
        int powderId,
        String oxideName,
        double percentage
) {}
