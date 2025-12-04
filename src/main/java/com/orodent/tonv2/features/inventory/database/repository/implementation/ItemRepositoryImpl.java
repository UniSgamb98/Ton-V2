package com.orodent.tonv2.features.inventory.database.repository.implementation;

import com.orodent.tonv2.features.inventory.database.model.Item;
import com.orodent.tonv2.features.inventory.database.repository.ItemRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemRepositoryImpl implements ItemRepository {

    private final Connection conn;

    public ItemRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Item findByCode(String code) {
        String sql = "SELECT * FROM item WHERE code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Item(rs.getInt("id"), rs.getString("code"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Item> findAll() {
        String sql = "SELECT id, code FROM item ORDER BY code ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Item> items = new ArrayList<>();

            while (rs.next()) {
                items.add(new Item(
                        rs.getInt("id"),
                        rs.getString("code")
                ));
            }

            return items;

        } catch (SQLException e) {
            throw new RuntimeException("Errore findAll su item", e);
        }
    }


    @Override
    public Item insert(String code) {
        String sql = "INSERT INTO item (code) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Item(keys.getInt(1), code);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
