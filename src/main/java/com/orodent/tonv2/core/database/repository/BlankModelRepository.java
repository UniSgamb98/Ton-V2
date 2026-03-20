package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.BlankModel;

import java.util.List;

public interface BlankModelRepository {
    List<BlankModel> findAll();
    BlankModel findById(int id);
    BlankModel insert(String code, double diameterMm, double superiorOvermaterialDefaultMm, double inferiorOvermaterialDefaultMm, double pressureKgCm2, double gramsPerMm, int numLayers);
}
