package com.orodent.tonv2.features.production.repository;

import com.orodent.tonv2.features.production.model.PowderOxide;

import java.util.List;

public interface PowderOxideRepository {
    List<PowderOxide> findByPowder(int powderId);
    PowderOxide save(PowderOxide oxide);
    boolean delete(int id);
}
