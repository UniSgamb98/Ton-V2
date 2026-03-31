package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.util.List;
import java.util.Locale;

public class CompositionDraftStateService {

    public String buildLayersSignature(List<LayerDraft> layers) {
        StringBuilder sb = new StringBuilder();
        for (LayerDraft layer : layers) {
            sb.append("L").append(layer.layerNumber()).append(':');
            layer.ingredients().forEach(ingredient -> sb
                    .append(ingredient.powderId())
                    .append('=')
                    .append(String.format(Locale.ROOT, "%.6f", ingredient.percentage()))
                    .append(';'));
            sb.append('|');
        }
        return sb.toString();
    }
}
