package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Stock;
import com.orodent.tonv2.core.database.repository.StockRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    public List<Stock> findByLotAll(int lotId) {
        String sql = """
        SELECT id, lot_id, depot_id, quantity
        FROM stock
        WHERE lot_id = ?
        ORDER BY depot_id
        """;

        List<Stock> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lotId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Stock(
                            rs.getInt("id"),
                            rs.getInt("lot_id"),
                            rs.getInt("depot_id"),
                            rs.getInt("quantity")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }


    @Override
    public int getQuantityByItemAndDepot(int itemId, int depotId) {

        String sql = """
        SELECT SUM(s.quantity) AS qty
        FROM stock s
        JOIN lot l ON l.id = s.lot_id
        WHERE l.item_id = ?
          AND s.depot_id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.setInt(2, depotId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("qty");
            }
            return 0;

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
