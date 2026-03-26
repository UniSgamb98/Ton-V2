package com.orodent.tonv2.features.laboratory.diskmodel.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;

import java.util.List;

public class CreateDiskModelService {

    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final BlankModelHeightOvermaterialRepository overmaterialRepo;

    public CreateDiskModelService(BlankModelRepository blankModelRepo,
                                  BlankModelLayerRepository blankModelLayerRepo,
                                  BlankModelHeightOvermaterialRepository overmaterialRepo) {
        this.blankModelRepo = blankModelRepo;
        this.blankModelLayerRepo = blankModelLayerRepo;
        this.overmaterialRepo = overmaterialRepo;
    }

    public void createDiskModel(CreateDiskModelData modelData,
                                List<LayerData> layers,
                                List<HeightRangeData> ranges) {
        validateLayers(modelData.numLayers(), layers);
        validateRanges(ranges);

        BlankModel model = blankModelRepo.insert(
                modelData.code(),
                modelData.diameter(),
                modelData.defaultSuperiorOvermaterial(),
                modelData.defaultInferiorOvermaterial(),
                modelData.pressure(),
                modelData.gramsPerMm(),
                modelData.numLayers()
        );

        for (LayerData layer : layers) {
            blankModelLayerRepo.insert(new BlankModelLayer(model.id(), layer.layerNumber(), layer.percentage()));
        }

        for (HeightRangeData range : ranges) {
            overmaterialRepo.insert(new BlankModelHeightOvermaterial(
                    0,
                    model.id(),
                    range.minHeight(),
                    range.maxHeight(),
                    range.superiorOvermaterial(),
                    range.inferiorOvermaterial()
            ));
        }
    }

    private void validateLayers(int expectedNumLayers, List<LayerData> layers) {
        if (layers.size() != expectedNumLayers) {
            throw new IllegalArgumentException("Inserisci la percentuale per tutti i layer del modello.");
        }

        double sum = layers.stream()
                .mapToDouble(LayerData::percentage)
                .sum();

        if (Math.abs(sum - 100.0) > 0.0001) {
            throw new IllegalArgumentException("La somma delle percentuali layer deve essere esattamente 100%.");
        }
    }

    private void validateRanges(List<HeightRangeData> ranges) {
        for (HeightRangeData range : ranges) {
            if (range.maxHeight() <= range.minHeight()) {
                throw new IllegalArgumentException("Ogni fascia deve avere max altezza maggiore della min altezza.");
            }
        }
    }

    public record CreateDiskModelData(String code,
                                      double diameter,
                                      double defaultSuperiorOvermaterial,
                                      double defaultInferiorOvermaterial,
                                      double pressure,
                                      double gramsPerMm,
                                      int numLayers) {
    }

    public record LayerData(int layerNumber, double percentage) {
    }

    public record HeightRangeData(double minHeight,
                                  double maxHeight,
                                  double superiorOvermaterial,
                                  double inferiorOvermaterial) {
    }
}
