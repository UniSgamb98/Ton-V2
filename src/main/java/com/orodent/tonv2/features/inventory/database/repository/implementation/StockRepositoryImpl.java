package com.orodent.tonv2.features.inventory.database.repository.implementation;

import com.orodent.tonv2.features.inventory.database.model.Stock;
import com.orodent.tonv2.features.inventory.database.repository.StockRepository;

import java.sql.*;

public class StockRepositoryImpl implements StockRepository {

    private final Connection conn;

    public StockRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Stock find(int lotId, int depotId) {
        String sql = "SELECT * FROM stock WHERE lot_id = ? AND depot_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lotId);
            ps.setInt(2, depotId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Stock(
                        rs.getInt("id"),
                        rs.getInt("lot_id"),
                        rs.getInt("depot_id"),
                        rs.getInt("quantity")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Stock upsert(int lotId, int depotId, int quantity) {
        Stock existing = find(lotId, depotId);

        if (existing == null) {
            String sql = "INSERT INTO stock (lot_id, depot_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, lotId);
                ps.setInt(2, depotId);
                ps.setInt(3, quantity);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                return new Stock(keys.getInt(1), lotId, depotId, quantity);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // update
        String sql = "UPDATE stock SET quantity = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, existing.id());
            ps.executeUpdate();
            return new Stock(existing.id(), lotId, depotId, quantity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
