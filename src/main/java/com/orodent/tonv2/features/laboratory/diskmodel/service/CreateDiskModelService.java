package com.orodent.tonv2.features.laboratory.diskmodel.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelService {

    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final BlankModelHeightOvermaterialRepository overmaterialRepo;
    private final CompositionRepository compositionRepo;

    public CreateDiskModelService(BlankModelRepository blankModelRepo,
                                  BlankModelLayerRepository blankModelLayerRepo,
                                  BlankModelHeightOvermaterialRepository overmaterialRepo,
                                  CompositionRepository compositionRepo) {
        this.blankModelRepo = blankModelRepo;
        this.blankModelLayerRepo = blankModelLayerRepo;
        this.overmaterialRepo = overmaterialRepo;
        this.compositionRepo = compositionRepo;
    }

    public BlankModel createDiskModel(CreateDiskModelData modelData,
                                      List<LayerData> layers,
                                      List<HeightRangeData> ranges) {
        validateModelData(modelData);
        validateLayers(modelData.numLayers(), layers);
        List<HeightRangeData> normalizedRanges = validateAndNormalizeRanges(ranges);

        BlankModel model = blankModelRepo.insert(
                modelData.code().trim(),
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

        for (HeightRangeData range : normalizedRanges) {
            overmaterialRepo.insert(new BlankModelHeightOvermaterial(
                    0,
                    model.id(),
                    range.minHeight(),
                    range.maxHeight(),
                    range.superiorOvermaterial(),
                    range.inferiorOvermaterial()
            ));
        }

        return model;
    }

    public VersionedSaveResult createDiskModelVersionFrom(int sourceBlankModelId,
                                                          CreateDiskModelData modelData,
                                                          List<LayerData> layers,
                                                          List<HeightRangeData> ranges) {
        BlankModel newModel = createDiskModel(modelData, layers, ranges);
        int copiedAssociations = compositionRepo.copyBlankModelAssociations(sourceBlankModelId, newModel.id());
        return new VersionedSaveResult(newModel.id(), copiedAssociations);
    }

    private void validateModelData(CreateDiskModelData modelData) {
        if (modelData.code() == null || modelData.code().isBlank()) {
            throw new IllegalArgumentException("Codice modello è obbligatorio.");
        }

        if (modelData.diameter() == null) {
            throw new IllegalArgumentException("Diametro è obbligatorio.");
        }
        if (modelData.diameter() <= 0) {
            throw new IllegalArgumentException("Diametro deve essere maggiore di 0.");
        }

        if (modelData.defaultSuperiorOvermaterial() == null) {
            throw new IllegalArgumentException("Overmaterial superiore default è obbligatorio.");
        }
        if (modelData.defaultSuperiorOvermaterial() < 0) {
            throw new IllegalArgumentException("Overmaterial superiore default non può essere negativo.");
        }

        if (modelData.defaultInferiorOvermaterial() == null) {
            throw new IllegalArgumentException("Overmaterial inferiore default è obbligatorio.");
        }
        if (modelData.defaultInferiorOvermaterial() < 0) {
            throw new IllegalArgumentException("Overmaterial inferiore default non può essere negativo.");
        }

        if (modelData.pressure() == null) {
            throw new IllegalArgumentException("Pressione è obbligatorio.");
        }
        if (modelData.pressure() <= 0) {
            throw new IllegalArgumentException("Pressione deve essere maggiore di 0.");
        }

        if (modelData.gramsPerMm() == null) {
            throw new IllegalArgumentException("Grammi per mm è obbligatorio.");
        }
        if (modelData.gramsPerMm() <= 0) {
            throw new IllegalArgumentException("Grammi per mm deve essere maggiore di 0.");
        }

        if (modelData.numLayers() == null) {
            throw new IllegalArgumentException("Numero strati è obbligatorio.");
        }
        if (modelData.numLayers() <= 0) {
            throw new IllegalArgumentException("Numero strati deve essere maggiore di 0.");
        }
    }

    private void validateLayers(int expectedNumLayers, List<LayerData> layers) {
        if (layers.size() != expectedNumLayers) {
            throw new IllegalArgumentException("Inserisci la percentuale per tutti i layer del modello.");
        }

        double sum = 0;
        for (LayerData layer : layers) {
            if (layer.percentage() == null) {
                throw new IllegalArgumentException("Percentuale layer " + layer.layerNumber() + " è obbligatoria.");
            }
            if (layer.percentage() <= 0) {
                throw new IllegalArgumentException("Percentuale layer " + layer.layerNumber() + " deve essere maggiore di 0.");
            }
            sum += layer.percentage();
        }

        if (Math.abs(sum - 100.0) > 0.0001) {
            throw new IllegalArgumentException("La somma delle percentuali layer deve essere esattamente 100%.");
        }
    }

    private List<HeightRangeData> validateAndNormalizeRanges(List<HeightRangeData> ranges) {
        List<HeightRangeData> normalized = new ArrayList<>();

        for (HeightRangeData range : ranges) {
            if (range.isEmpty()) {
                continue;
            }

            if (!range.isComplete()) {
                throw new IllegalArgumentException("Ogni fascia altezza deve avere tutti i campi compilati.");
            }

            if (range.minHeight() < 0) {
                throw new IllegalArgumentException("Min altezza fascia non può essere negativo.");
            }
            if (range.maxHeight() <= 0) {
                throw new IllegalArgumentException("Max altezza fascia deve essere maggiore di 0.");
            }
            if (range.superiorOvermaterial() < 0) {
                throw new IllegalArgumentException("Overmaterial superiore fascia non può essere negativo.");
            }
            if (range.inferiorOvermaterial() < 0) {
                throw new IllegalArgumentException("Overmaterial inferiore fascia non può essere negativo.");
            }
            if (range.maxHeight() <= range.minHeight()) {
                throw new IllegalArgumentException("Ogni fascia deve avere max altezza maggiore della min altezza.");
            }

            normalized.add(range);
        }

        return normalized;
    }

    public record CreateDiskModelData(String code,
                                      Double diameter,
                                      Double defaultSuperiorOvermaterial,
                                      Double defaultInferiorOvermaterial,
                                      Double pressure,
                                      Double gramsPerMm,
                                      Integer numLayers) {
    }

    public record LayerData(int layerNumber, Double percentage) {
    }

    public record HeightRangeData(Double minHeight,
                                  Double maxHeight,
                                  Double superiorOvermaterial,
                                  Double inferiorOvermaterial) {
        public boolean isEmpty() {
            return minHeight == null
                    && maxHeight == null
                    && superiorOvermaterial == null
                    && inferiorOvermaterial == null;
        }

        public boolean isComplete() {
            return minHeight != null
                    && maxHeight != null
                    && superiorOvermaterial != null
                    && inferiorOvermaterial != null;
        }
    }

    public record VersionedSaveResult(int newBlankModelId, int copiedCompositionAssociations) {
    }
}
