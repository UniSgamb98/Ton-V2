package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductionRepositoryImpl implements ProductionRepository {
    private final Connection conn;

    public ProductionRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public int insertProductionOrder(int productId, int compositionId, int blankModelId, LocalDate productionDate, String notes) {
        String sql = """
                INSERT INTO production_order (product_id, composition_id, blank_model_id, production_date, notes)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setInt(2, compositionId);
            ps.setInt(3, blankModelId);
            ps.setDate(4, Date.valueOf(productionDate));
            ps.setString(5, notes);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Nessun ID restituito per production_order");
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento production_order", e);
        }
    }

    @Override
    public void insertProductionOrderLine(int productionOrderId, int itemId, int quantity) {
        String sql = """
                INSERT INTO production_order_line (production_order_id, item_id, quantity)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productionOrderId);
            ps.setInt(2, itemId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento production_order_line", e);
        }
    }

    @Override
    public List<ProducedDiskRow> findProducedDiskRows() {
        String sql = """
                SELECT i.id AS item_id, i.code AS item_code, SUM(pol.quantity) AS total_qty
                FROM production_order_line pol
                JOIN production_order po ON po.id = pol.production_order_id
                JOIN item i ON i.id = pol.item_id
                LEFT JOIN production_order_firing pof ON pof.production_order_id = po.id
                WHERE pof.production_order_id IS NULL
                GROUP BY i.id, i.code
                ORDER BY i.code ASC
                """;

        List<ProducedDiskRow> rows = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new ProducedDiskRow(
                        rs.getInt("item_id"),
                        rs.getString("item_code"),
                        rs.getInt("total_qty")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento dei dischi prodotti.", e);
        }

        return rows;
    }

    @Override
    public List<CompositionRankingRow> findCompositionRankingRows() {
        String sql = """
                SELECT a.composition_id,
                       a.available_qty,
                       COALESCE(fs.distinct_furnaces_used, 0) AS distinct_furnaces_used,
                       COALESCE(fs.total_firings, 0) AS total_firings
                FROM (
                    SELECT po.composition_id AS composition_id, SUM(pol.quantity) AS available_qty
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    LEFT JOIN production_order_firing pof ON pof.production_order_id = po.id
                    WHERE pof.production_order_id IS NULL
                    GROUP BY po.composition_id
                ) a
                LEFT JOIN (
                    SELECT po.composition_id AS composition_id,
                           COUNT(DISTINCT f.furnace) AS distinct_furnaces_used,
                           COUNT(*) AS total_firings
                    FROM production_order po
                    JOIN production_order_firing pof ON pof.production_order_id = po.id
                    JOIN firing f ON f.id = pof.firing_id
                    GROUP BY po.composition_id
                ) fs ON fs.composition_id = a.composition_id
                ORDER BY distinct_furnaces_used ASC, total_firings ASC, a.available_qty DESC, a.composition_id ASC
                """;

        List<CompositionRankingRow> rows = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CompositionRankingRow(
                        rs.getInt("composition_id"),
                        rs.getInt("available_qty"),
                        rs.getInt("distinct_furnaces_used"),
                        rs.getInt("total_firings")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento classifica composizioni.", e);
        }

        return rows;
    }

    @Override
    public List<FurnaceItemSuggestionRow> findFurnaceItemSuggestionRows(String furnaceValue, String furnaceDisplayValue) {
        String sql = """
                SELECT ai.item_id,
                       ai.item_code,
                       ai.composition_id,
                       ai.available_qty,
                       fh.avg_furnace_temp AS suggested_temperature
                FROM (
                    SELECT pol.item_id AS item_id,
                           i.code AS item_code,
                           po.composition_id AS composition_id,
                           SUM(pol.quantity) AS available_qty
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    JOIN item i ON i.id = pol.item_id
                    LEFT JOIN production_order_firing pof ON pof.production_order_id = po.id
                    WHERE pof.production_order_id IS NULL
                    GROUP BY pol.item_id, i.code, po.composition_id
                ) ai
                JOIN (
                    SELECT pol.item_id AS item_id,
                           po.composition_id AS composition_id,
                           AVG(f.max_temperature) AS avg_furnace_temp
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    JOIN production_order_firing pof ON pof.production_order_id = po.id
                    JOIN firing f ON f.id = pof.firing_id
                    WHERE (f.furnace = ? OR f.furnace = ?)
                      AND f.max_temperature IS NOT NULL
                    GROUP BY pol.item_id, po.composition_id
                ) fh
                    ON fh.item_id = ai.item_id
                   AND fh.composition_id = ai.composition_id
                ORDER BY ai.composition_id ASC, ai.item_code ASC
                """;

        List<FurnaceItemSuggestionRow> rows = new ArrayList<>();
        List<RawFurnaceItemSuggestionRow> rawRows = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, furnaceValue);
            ps.setString(2, furnaceDisplayValue);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Number suggestedTemperature = (Number) rs.getObject("suggested_temperature");
                    rawRows.add(new RawFurnaceItemSuggestionRow(
                            rs.getInt("item_id"),
                            rs.getString("item_code"),
                            rs.getInt("composition_id"),
                            rs.getInt("available_qty"),
                            suggestedTemperature == null ? null : suggestedTemperature.doubleValue()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento suggerimenti forno selezionato.", e);
        }

        for (RawFurnaceItemSuggestionRow rawRow : rawRows) {
            Integer compositionAverage = computeCompositionAverage(rawRows, rawRow.compositionId());
            rows.add(new FurnaceItemSuggestionRow(
                    rawRow.itemId(),
                    rawRow.itemCode(),
                    rawRow.compositionId(),
                    rawRow.availableQuantity(),
                    rawRow.suggestedTemperature() == null ? null : Math.round(rawRow.suggestedTemperature().floatValue()),
                    compositionAverage
            ));
        }

        return rows;
    }

    private Integer computeCompositionAverage(List<RawFurnaceItemSuggestionRow> rows, int compositionId) {
        int total = 0;
        int count = 0;
        for (RawFurnaceItemSuggestionRow row : rows) {
            if (row.compositionId() != compositionId || row.suggestedTemperature() == null) {
                continue;
            }
            total += Math.round(row.suggestedTemperature().floatValue());
            count++;
        }
        return count == 0 ? null : Math.round((float) total / count);
    }

    private record RawFurnaceItemSuggestionRow(int itemId,
                                               String itemCode,
                                               int compositionId,
                                               int availableQuantity,
                                               Double suggestedTemperature) {
    }
}
