package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.util.List;

public class PresinteringService {
    private final ProductionRepository productionRepo;

    public PresinteringService (ProductionRepository productionRepo){
        this.productionRepo = productionRepo;
    }

    public List<ProductionRepository.ProducedDiskRow> loadProducedDisks() {
        return productionRepo.findProducedDiskRows();
    }
}
