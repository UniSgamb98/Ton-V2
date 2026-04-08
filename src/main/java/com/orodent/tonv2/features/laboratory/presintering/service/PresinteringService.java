package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.model.Firing;
import com.orodent.tonv2.core.database.repository.FiringRepository;
import com.orodent.tonv2.core.database.repository.FurnaceRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PresinteringService {
    private static final Path SNAPSHOT_PATH = Path.of(
            System.getProperty("user.home"),
            ".ton",
            "presintering-snapshot.bin"
    );

    private final ProductionRepository productionRepo;
    private final FurnaceRepository furnaceRepo;
    private final FiringRepository firingRepo;
    private final LotRepository lotRepo;
    private final Connection conn;

    public PresinteringService(ProductionRepository productionRepo,
                               FurnaceRepository furnaceRepo,
                               FiringRepository firingRepo,
                               LotRepository lotRepo,
                               Connection conn) {
        this.productionRepo = productionRepo;
        this.furnaceRepo = furnaceRepo;
        this.firingRepo = firingRepo;
        this.lotRepo = lotRepo;
        this.conn = conn;
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

    public void clearSnapshot() {
        try {
            Files.deleteIfExists(SNAPSHOT_PATH);
        } catch (Exception ignored) {
            // Best effort delete.
        }
    }

    public ConfirmationResult confirmPresintering(int furnaceId,
                                                  String furnaceName,
                                                  LocalDate firingDate,
                                                  Integer maxTemperature,
                                                  Map<Integer, Integer> plannedItemsByItemId) {
        if (furnaceName == null || furnaceName.isBlank()) {
            throw new IllegalArgumentException("Forno non valido.");
        }
        if (firingDate == null) {
            throw new IllegalArgumentException("Data partenza obbligatoria.");
        }
        if (maxTemperature == null || maxTemperature <= 0) {
            throw new IllegalArgumentException("Temperatura massima non valida.");
        }
        if (plannedItemsByItemId == null || plannedItemsByItemId.isEmpty()) {
            throw new IllegalArgumentException("Nessun item pianificato da confermare.");
        }

        boolean previousAutoCommit;
        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile iniziare la transazione di conferma presinterizzazione.", e);
        }

        try {
            Firing firing = firingRepo.insert(firingDate, furnaceName, maxTemperature, null, "Presinterizzazione forno id=" + furnaceId);
            Set<Integer> productionOrdersToLink = new LinkedHashSet<>();

            for (Map.Entry<Integer, Integer> plannedEntry : plannedItemsByItemId.entrySet()) {
                int itemId = plannedEntry.getKey();
                int requestedQty = plannedEntry.getValue() == null ? 0 : plannedEntry.getValue();
                if (requestedQty <= 0) {
                    continue;
                }

                List<ProductionRepository.OpenProductionOrderLineRow> openOrderLines = productionRepo.findOpenProductionOrderLinesByItem(itemId);
                int coveredQty = 0;
                for (ProductionRepository.OpenProductionOrderLineRow orderLine : openOrderLines) {
                    if (coveredQty >= requestedQty) {
                        break;
                    }
                    productionOrdersToLink.add(orderLine.productionOrderId());
                    coveredQty += orderLine.quantity();
                }

                if (coveredQty < requestedQty) {
                    throw new IllegalStateException("Quantità pianificata non coerente per item " + itemId + ".");
                }

                String lotCode = buildRandomLotCode(firing.id(), itemId);
                lotRepo.insert(lotCode, firing.id());
            }

            for (Integer productionOrderId : productionOrdersToLink) {
                productionRepo.insertProductionOrderFiring(productionOrderId, firing.id());
            }

            conn.commit();
            conn.setAutoCommit(previousAutoCommit);
            return new ConfirmationResult(firing.id(), productionOrdersToLink.size(), plannedItemsByItemId.size());
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
                // best effort rollback
            }
            try {
                conn.setAutoCommit(previousAutoCommit);
            } catch (Exception ignored) {
                // ignore
            }
            throw new RuntimeException("Errore durante conferma presinterizzazione.", e);
        }
    }

    private String buildRandomLotCode(int firingId, int itemId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "LOT-F" + firingId + "-I" + itemId + "-" + suffix;
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

    public record ConfirmationResult(int firingId, int linkedProductionOrders, int lotCount) {
    }
}
