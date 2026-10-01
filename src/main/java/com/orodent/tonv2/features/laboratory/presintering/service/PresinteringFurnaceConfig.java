package com.orodent.tonv2.features.laboratory.presintering.service;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public record PresinteringFurnaceConfig(Integer maxTemperature,
                                        LocalDate departureDate,
                                        String lotCode) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
