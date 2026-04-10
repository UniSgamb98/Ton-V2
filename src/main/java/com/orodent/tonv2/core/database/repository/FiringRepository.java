package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Firing;

import java.time.LocalDate;

public interface FiringRepository {
    Firing insert(LocalDate firingDate, String furnace, Integer maxTemperature, String notes);
    Integer findLatestId();
}
