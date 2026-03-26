package com.orodent.tonv2.features.laboratory.presintering.controller;

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

        loadProducedDisks();
    }

    private void loadProducedDisks() {
        try {
            List<ProductionRepository.ProducedDiskRow> rows = service.loadProducedDisks();
            view.setProducedDisks(rows);
            view.setFeedback("", false);
        } catch (Exception e) {
            view.setProducedDisks(List.of());
            view.setFeedback("Errore durante il caricamento dischi prodotti.", true);
        }
    }
}
