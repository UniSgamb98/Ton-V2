package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.util.List;

public class PresinteringService {

    public List<ProductionRepository.ProducedDiskRow> loadProducedDisks(ProductionRepository productionRepo) {
        return productionRepo.findProducedDiskRows();
    }
}
