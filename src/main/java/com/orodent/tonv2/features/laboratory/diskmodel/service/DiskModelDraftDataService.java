package com.orodent.tonv2.features.laboratory.diskmodel.service;

import com.orodent.tonv2.core.ui.form.FieldParsers;
import com.orodent.tonv2.features.laboratory.diskmodel.view.CreateDiskModelView;

import java.util.ArrayList;
import java.util.List;

public class DiskModelDraftDataService {

    public String buildLayerSignature(List<CreateDiskModelView.LayerPercentageDraft> layerDrafts) {
        StringBuilder sb = new StringBuilder();
        layerDrafts.forEach(layer -> sb
                .append(layer.layerNumber())
                .append('=')
                .append(normalize(layer.percentage()))
                .append(';'));
        return sb.toString();
    }

    public String buildRangeSignature(List<CreateDiskModelView.HeightRangeDraft> rangeDrafts) {
        StringBuilder sb = new StringBuilder();
        rangeDrafts.forEach(range -> sb
                .append(normalize(range.minHeight()))
                .append('|')
                .append(normalize(range.maxHeight()))
                .append('|')
                .append(normalize(range.superiorOvermaterial()))
                .append('|')
                .append(normalize(range.inferiorOvermaterial()))
                .append(';'));
        return sb.toString();
    }

    public List<CreateDiskModelService.LayerData> parseLayers(List<CreateDiskModelView.LayerPercentageDraft> layerDrafts) {
        List<CreateDiskModelService.LayerData> layers = new ArrayList<>();

        for (CreateDiskModelView.LayerPercentageDraft draft : layerDrafts) {
            Double percentage = FieldParsers.parseDouble(draft.percentage(), "Percentuale layer " + draft.layerNumber());
            layers.add(new CreateDiskModelService.LayerData(draft.layerNumber(), percentage));
        }

        return layers;
    }

    public List<CreateDiskModelService.HeightRangeData> parseRanges(List<CreateDiskModelView.HeightRangeDraft> rangeDrafts) {
        List<CreateDiskModelService.HeightRangeData> ranges = new ArrayList<>();

        for (CreateDiskModelView.HeightRangeDraft draft : rangeDrafts) {
            ranges.add(new CreateDiskModelService.HeightRangeData(
                    FieldParsers.parseDouble(draft.minHeight(), "Min altezza fascia"),
                    FieldParsers.parseDouble(draft.maxHeight(), "Max altezza fascia"),
                    FieldParsers.parseDouble(draft.superiorOvermaterial(), "Overmaterial superiore fascia"),
                    FieldParsers.parseDouble(draft.inferiorOvermaterial(), "Overmaterial inferiore fascia")
            ));
        }

        return ranges;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
