package com.orodent.tonv2.core.database.model;

import java.time.LocalDate;

public record Firing(
        int id,
        LocalDate firingDate,
        String furnace,
        Integer maxTemperature,
        Integer durationMinutes,
        String notes
) {}
