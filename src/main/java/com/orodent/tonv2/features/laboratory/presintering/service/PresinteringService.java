package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.FurnaceRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.util.List;

public class PresinteringService {
    private final ProductionRepository productionRepo;
    private final FurnaceRepository furnaceRepo;

    public PresinteringService(ProductionRepository productionRepo,
                               FurnaceRepository furnaceRepo) {
        this.productionRepo = productionRepo;
        this.furnaceRepo = furnaceRepo;
    }

    public List<ProductionRepository.ProducedDiskRow> loadProducedDisks() {
        return productionRepo.findProducedDiskRows();
    }

    public List<Furnace> loadFurnaces() {
        return furnaceRepo.findAll();
    }
}
