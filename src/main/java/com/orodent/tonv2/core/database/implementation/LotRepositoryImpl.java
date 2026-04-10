package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Lot;
import com.orodent.tonv2.core.database.repository.LotRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LotRepositoryImpl implements LotRepository {

    private final Connection conn;

    public LotRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Lot findByCodeAndItem(String lotCode, int itemId) {
        String sql = """
                SELECT DISTINCT l.id, l.code, l.firing_id
                FROM lot l
                JOIN production_order_line_firing polf ON polf.firing_id = l.firing_id
                WHERE l.code = ?
                  AND polf.item_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lotCode);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Lot(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getInt("firing_id")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Lot> findByItem(int itemId) {
        String sql = """
                SELECT DISTINCT l.id, l.code, l.firing_id
                FROM lot l
                JOIN production_order_line_firing polf ON polf.firing_id = l.firing_id
                WHERE polf.item_id = ?
                ORDER BY l.code
                """;

        List<Lot> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Lot(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getInt("firing_id")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }



    @Override
    public Lot insert(String lotCode, int firingId) {
        String sql = "INSERT INTO lot (code, firing_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lotCode);
            ps.setInt(2, firingId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Lot(keys.getInt(1), lotCode, firingId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
