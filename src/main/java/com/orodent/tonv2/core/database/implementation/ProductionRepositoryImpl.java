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
                SELECT rl.item_id,
                       i.code AS item_code,
                       p.code AS product_name,
                       SUM(rl.remaining_qty) AS total_qty
                FROM (
                    SELECT pol.production_order_id,
                           pol.item_id,
                           CASE
                               WHEN pol.quantity - COALESCE(polf.assigned_qty, 0) > 0
                                   THEN pol.quantity - COALESCE(polf.assigned_qty, 0)
                               ELSE 0
                           END AS remaining_qty
                    FROM production_order_line pol
                    LEFT JOIN (
                        SELECT production_order_id, item_id, SUM(quantity) AS assigned_qty
                        FROM production_order_line_firing
                        GROUP BY production_order_id, item_id
                    ) polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                ) rl
                JOIN item i ON i.id = rl.item_id
                JOIN product p ON p.id = i.product_id
                WHERE rl.remaining_qty > 0
                GROUP BY rl.item_id, i.code, p.code
                ORDER BY i.code ASC
                """;

        List<ProducedDiskRow> rows = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new ProducedDiskRow(
                        rs.getInt("item_id"),
                        rs.getString("item_code"),
                        rs.getString("product_name"),
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
                       p.code AS product_name,
                       a.available_qty,
                       COALESCE(fs.distinct_furnaces_used, 0) AS distinct_furnaces_used,
                       COALESCE(fs.total_firings, 0) AS total_firings
                FROM (
                    SELECT po.composition_id AS composition_id,
                           SUM(pol.quantity - COALESCE(polf.assigned_qty, 0)) AS available_qty
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    LEFT JOIN (
                        SELECT production_order_id, item_id, SUM(quantity) AS assigned_qty
                        FROM production_order_line_firing
                        GROUP BY production_order_id, item_id
                    ) polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                    WHERE pol.quantity - COALESCE(polf.assigned_qty, 0) > 0
                    GROUP BY po.composition_id
                ) a
                JOIN composition c ON c.id = a.composition_id
                JOIN product p ON p.id = c.product_id
                LEFT JOIN (
                    SELECT po.composition_id AS composition_id,
                           COUNT(DISTINCT f.furnace) AS distinct_furnaces_used,
                           COUNT(*) AS total_firings
                    FROM production_order po
                    JOIN production_order_line pol ON pol.production_order_id = po.id
                    JOIN production_order_line_firing polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                    JOIN firing f ON f.id = polf.firing_id
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
                        rs.getString("product_name"),
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
                           SUM(pol.quantity - COALESCE(polf.assigned_qty, 0)) AS available_qty
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    JOIN item i ON i.id = pol.item_id
                    LEFT JOIN (
                        SELECT production_order_id, item_id, SUM(quantity) AS assigned_qty
                        FROM production_order_line_firing
                        GROUP BY production_order_id, item_id
                    ) polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                    WHERE pol.quantity - COALESCE(polf.assigned_qty, 0) > 0
                    GROUP BY pol.item_id, i.code, po.composition_id
                ) ai
                JOIN (
                    SELECT pol.item_id AS item_id,
                           po.composition_id AS composition_id,
                           AVG(f.max_temperature) AS avg_furnace_temp
                    FROM production_order_line pol
                    JOIN production_order po ON po.id = pol.production_order_id
                    JOIN production_order_line_firing polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                    JOIN firing f ON f.id = polf.firing_id
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

    @Override
    public List<OpenProductionOrderLineRow> findOpenProductionOrderLinesByItem(int itemId) {
        String sql = """
                SELECT rl.production_order_id,
                       rl.item_id,
                       rl.remaining_qty AS quantity
                FROM (
                    SELECT pol.production_order_id,
                           pol.item_id,
                           CASE
                               WHEN pol.quantity - COALESCE(polf.assigned_qty, 0) > 0
                                   THEN pol.quantity - COALESCE(polf.assigned_qty, 0)
                               ELSE 0
                           END AS remaining_qty
                    FROM production_order_line pol
                    LEFT JOIN (
                        SELECT production_order_id, item_id, SUM(quantity) AS assigned_qty
                        FROM production_order_line_firing
                        GROUP BY production_order_id, item_id
                    ) polf
                        ON polf.production_order_id = pol.production_order_id
                       AND polf.item_id = pol.item_id
                    WHERE pol.item_id = ?
                ) rl
                WHERE rl.remaining_qty > 0
                ORDER BY rl.production_order_id ASC
                """;

        List<OpenProductionOrderLineRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OpenProductionOrderLineRow(
                            rs.getInt("production_order_id"),
                            rs.getInt("item_id"),
                            rs.getInt("quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento ordini aperti per item.", e);
        }
        return rows;
    }

    @Override
    public void insertProductionOrderLineFiring(int productionOrderId, int itemId, int firingId, int quantity) {
        String sql = """
                INSERT INTO production_order_line_firing (production_order_id, item_id, firing_id, quantity)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productionOrderId);
            ps.setInt(2, itemId);
            ps.setInt(3, firingId);
            ps.setInt(4, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento production_order_line_firing.", e);
        }
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
