package com.orodent.tonv2.features.inventory.database.repository.implementation;

import com.orodent.tonv2.features.inventory.database.model.Lot;
import com.orodent.tonv2.features.inventory.database.repository.LotRepository;

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
    public List<Lot> findByItem(int itemId) {
        String sql = "SELECT id, lot_code AS code, item_id FROM lot WHERE item_id = ? ORDER BY lot_code";

        List<Lot> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // uso "code" nella resultset per mantenere compatibilità con i costruttori che
                    // si aspettano un campo chiamato 'code' (se il tuo Lot ha nomi diversi, adatta)
                    result.add(new Lot(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getInt("item_id")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
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
