package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.FurnaceRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PresinteringService {
    private static final Path SNAPSHOT_PATH = Path.of(
            System.getProperty("user.home"),
            ".ton",
            "presintering-snapshot.bin"
    );

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

    public List<ProductionRepository.CompositionRankingRow> loadCompositionRanking() {
        return productionRepo.findCompositionRankingRows();
    }

    public List<ProductionRepository.FurnaceItemSuggestionRow> loadFurnaceItemSuggestions(String selectedFurnaceName) {
        if (selectedFurnaceName == null || selectedFurnaceName.isBlank()) {
            return List.of();
        }

        String normalizedFurnace = selectedFurnaceName.replaceFirst("^Forno\\s+", "").trim();
        return productionRepo.findFurnaceItemSuggestionRows(normalizedFurnace, selectedFurnaceName);
    }

    public Optional<PresinteringPlanningSnapshot> loadValidSnapshot(List<ProductionRepository.ProducedDiskRow> dbRows) {
        if (!Files.exists(SNAPSHOT_PATH)) {
            return Optional.empty();
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(SNAPSHOT_PATH))) {
            Object object = in.readObject();
            if (!(object instanceof PresinteringPlanningSnapshot snapshot)) {
                return Optional.empty();
            }

            return isSnapshotValid(snapshot, dbRows) ? Optional.of(snapshot) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void saveSnapshot(PresinteringPlanningSnapshot snapshot) {
        try {
            Files.createDirectories(SNAPSHOT_PATH.getParent());
            PresinteringPlanningSnapshot snapshotToWrite = new PresinteringPlanningSnapshot(
                    snapshot.availableByItemId(),
                    snapshot.plannedByFurnace(),
                    snapshot.itemCodeById(),
                    Instant.now()
            );
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(SNAPSHOT_PATH))) {
                out.writeObject(snapshotToWrite);
            }
        } catch (Exception ignored) {
            // Best effort local snapshot.
        }
    }

    private boolean isSnapshotValid(PresinteringPlanningSnapshot snapshot,
                                    List<ProductionRepository.ProducedDiskRow> dbRows) {
        Map<Integer, Integer> dbTotals = new LinkedHashMap<>();
        for (ProductionRepository.ProducedDiskRow row : dbRows) {
            dbTotals.put(row.itemId(), row.totalQuantity());
        }

        for (Map.Entry<Integer, Integer> dbEntry : dbTotals.entrySet()) {
            int itemId = dbEntry.getKey();
            int dbQty = dbEntry.getValue();
            int availableQty = snapshot.availableByItemId().getOrDefault(itemId, 0);
            int plannedQty = snapshot.plannedByFurnace().values().stream()
                    .mapToInt(byItem -> byItem.getOrDefault(itemId, 0))
                    .sum();
            if (dbQty != availableQty + plannedQty) {
                return false;
            }
        }

        for (Map.Entry<Integer, Integer> entry : snapshot.availableByItemId().entrySet()) {
            if (!dbTotals.containsKey(entry.getKey()) && entry.getValue() > 0) {
                return false;
            }
        }
        return true;
    }
}
