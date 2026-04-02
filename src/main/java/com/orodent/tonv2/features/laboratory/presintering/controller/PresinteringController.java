package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;

import java.util.List;

public class PresinteringController {

    private final PresinteringView view;

    private final PresinteringService service;

    public PresinteringController(PresinteringView view,
                                  PresinteringService service) {
        this.view = view;
        this.service = service;

        loadData();
    }

    private void loadData() {
        try {
            List<ProductionRepository.ProducedDiskRow> producedDisks = service.loadProducedDisks();
            List<Furnace> furnaces = service.loadFurnaces();

            view.setProducedDisks(producedDisks);
            view.setFurnaces(furnaces);
            view.setFeedback("", false);
        } catch (Exception e) {
            view.setProducedDisks(List.of());
            view.setFurnaces(List.of());
            view.setFeedback("Errore durante il caricamento dati presinterizza.", true);
        }
    }
}
