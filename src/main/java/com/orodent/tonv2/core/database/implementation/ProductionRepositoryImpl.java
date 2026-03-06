package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductionRepositoryImpl implements ProductionRepository {
    private final Connection conn;

    public ProductionRepositoryImpl(Connection conn) {
        this.conn = conn;
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
