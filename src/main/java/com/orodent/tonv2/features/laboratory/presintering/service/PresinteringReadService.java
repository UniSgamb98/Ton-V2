package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.FiringRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.FurnaceRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ProductionRepositoryImpl;
import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplatePresetCodes;

import java.util.List;

public class PresinteringReadService {

    private final ConnectionProvider connectionProvider;
    private final TemplateEditorService templateEditorService;

    public PresinteringReadService(ConnectionProvider connectionProvider,
                                   TemplateEditorService templateEditorService) {
        this.connectionProvider = connectionProvider;
        this.templateEditorService = templateEditorService;
    }

    public InitialData loadInitialData() {
        DatabaseData databaseData = connectionProvider.withConnection(connection -> {
            ProductionRepository productionRepository = new ProductionRepositoryImpl(connection);
            return new DatabaseData(
                    productionRepository.findProducedDiskRows(),
                    new FurnaceRepositoryImpl(connection).findAll(),
                    productionRepository.findCompositionRankingRows(),
                    new FiringRepositoryImpl(connection).findLatestId()
            );
        });

        List<String> templateNames = templateEditorService.getSavedTemplates().stream()
                .filter(template -> TemplatePresetCodes.FIRING.equals(template.presetCode()))
                .map(TemplateEditorService.TemplateSnapshot::name)
                .toList();

        return new InitialData(
                databaseData.producedDisks(),
                databaseData.furnaces(),
                databaseData.compositionRanking(),
                databaseData.latestFiringId(),
                templateNames,
                templateEditorService.getLastPresinteringTemplateName()
        );
    }

    public List<ProductionRepository.FurnaceItemSuggestionRow> loadFurnaceItemSuggestions(String furnaceName) {
        if (furnaceName == null || furnaceName.isBlank()) {
            return List.of();
        }
        String normalizedFurnace = furnaceName.replaceFirst("^Forno\\s+", "").trim();
        return connectionProvider.withConnection(connection ->
                new ProductionRepositoryImpl(connection)
                        .findFurnaceItemSuggestionRows(normalizedFurnace, furnaceName)
        );
    }

    public record InitialData(
            List<ProductionRepository.ProducedDiskRow> producedDisks,
            List<Furnace> furnaces,
            List<ProductionRepository.CompositionRankingRow> compositionRanking,
            Integer latestFiringId,
            List<String> templateNames,
            String selectedTemplateName) {
    }

    private record DatabaseData(
            List<ProductionRepository.ProducedDiskRow> producedDisks,
            List<Furnace> furnaces,
            List<ProductionRepository.CompositionRankingRow> compositionRanking,
            Integer latestFiringId) {
    }
}
