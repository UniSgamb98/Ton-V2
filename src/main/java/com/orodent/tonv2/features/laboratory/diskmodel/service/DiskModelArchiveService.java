package com.orodent.tonv2.features.laboratory.diskmodel.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;

import java.util.List;

public class DiskModelArchiveService {

    private final BlankModelRepository blankModelRepository;
    private final BlankModelLayerRepository blankModelLayerRepository;
    private final BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepository;

    public DiskModelArchiveService(BlankModelRepository blankModelRepository,
                                   BlankModelLayerRepository blankModelLayerRepository,
                                   BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepository) {
        this.blankModelRepository = blankModelRepository;
        this.blankModelLayerRepository = blankModelLayerRepository;
        this.blankModelHeightOvermaterialRepository = blankModelHeightOvermaterialRepository;
    }

    public List<BlankModel> searchDiskModels(String codeFilter) {
        String normalized = codeFilter == null ? "" : codeFilter.trim().toLowerCase();

        return blankModelRepository.findAll().stream()
                .filter(model -> normalized.isBlank() || model.code().toLowerCase().contains(normalized))
                .toList();
    }

    public DiskModelSnapshot loadDiskModelSnapshot(int blankModelId) {
        BlankModel model = blankModelRepository.findById(blankModelId);
        if (model == null) {
            return null;
        }

        List<BlankModelLayer> layers = blankModelLayerRepository.findByBlankModelId(blankModelId);
        List<BlankModelHeightOvermaterial> ranges = blankModelHeightOvermaterialRepository.findByBlankModelId(blankModelId);

        return new DiskModelSnapshot(model, layers, ranges);
    }

    public record DiskModelSnapshot(BlankModel model,
                                    List<BlankModelLayer> layers,
                                    List<BlankModelHeightOvermaterial> ranges) {
    }
}
