package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.BlankModel;

import java.util.List;
import java.util.Optional;

public interface BlankModelRepository {
    List<BlankModel> findAll();
    BlankModel findById(int id);
    Optional<Integer> findMaxVersionByCode(String code);
    BlankModel insert(String code, int version, double diameterMm, double superiorOvermaterialDefaultMm, double inferiorOvermaterialDefaultMm, double pressureKgCm2, double gramsPerMm, int numLayers);
}
