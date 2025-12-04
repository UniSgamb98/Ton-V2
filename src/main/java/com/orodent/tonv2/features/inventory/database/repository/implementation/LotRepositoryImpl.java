package com.orodent.tonv2.features.inventory.database.repository.implementation;

import com.orodent.tonv2.features.inventory.database.model.Lot;
import com.orodent.tonv2.features.inventory.database.repository.LotRepository;

import java.sql.*;

public class LotRepositoryImpl implements LotRepository {

    private final Connection conn;

    public LotRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Lot findByCodeAndItem(String lotCode, int itemId) {
        String sql = "SELECT * FROM lot WHERE lot_code = ? AND item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lotCode);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Lot(
                        rs.getInt("id"),
                        rs.getString("lot_code"),
                        rs.getInt("item_id")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Lot insert(String lotCode, int itemId) {
        String sql = "INSERT INTO lot (lot_code, item_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lotCode);
            ps.setInt(2, itemId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Lot(keys.getInt(1), lotCode, itemId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
