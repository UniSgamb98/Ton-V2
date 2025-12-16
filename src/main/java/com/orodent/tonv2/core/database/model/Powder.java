package com.orodent.tonv2.core.database.model;

public record Powder(
        int id,
        String code,
        String name,
        Double strength,
        Double translucency,
        int yttria,
        String notes
) {}
