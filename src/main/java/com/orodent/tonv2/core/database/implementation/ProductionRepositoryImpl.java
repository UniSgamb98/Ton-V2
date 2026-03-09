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
                JOIN item i ON i.id = pol.item_id
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
}
