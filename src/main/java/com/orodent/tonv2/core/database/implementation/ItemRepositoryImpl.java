package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.repository.ItemRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemRepositoryImpl implements ItemRepository {

    private final Connection conn;

    public ItemRepositoryImpl(Connection conn) {
        this.conn = conn;
    }


    @Override
    public Item findById(int id) {
        String sql = "SELECT id, product_id, blank_model_id, height_mm FROM item WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapItem(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Item findByProductAndHeight(int productId, double heightMm) {
        String sql = "SELECT id, product_id, blank_model_id, height_mm FROM item WHERE product_id = ? AND height_mm = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setDouble(2, heightMm);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapItem(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Item> findByDepot(String depotName) {
        String sql = """
        SELECT DISTINCT i.id, i.product_id, i.blank_model_id, i.height_mm
        FROM item i
        JOIN production_order_line pol ON pol.item_id = i.id
        JOIN production_order po ON po.id = pol.production_order_id
        JOIN production_order_firing pof ON pof.production_order_id = po.id
        JOIN firing f ON f.id = pof.firing_id
        JOIN lot l ON l.firing_id = f.id
        JOIN stock s ON s.lot_id = l.id
        JOIN depot d ON d.id = s.depot_id
        WHERE d.name = ?
        AND s.quantity > 0
    """;

        List<Item> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, depotName);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapItem(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }


    @Override
    public List<Item> findAll() {
        String sql = "SELECT id, product_id, blank_model_id, height_mm FROM item ORDER BY product_id ASC, height_mm ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Item> items = new ArrayList<>();

            while (rs.next()) {
                items.add(mapItem(rs));
            }

            return items;

        } catch (SQLException e) {
            throw new RuntimeException("Errore findAll su item", e);
        }
    }

    @Override
    public List<Item> findByProduct(int productId) {
        String sql = "SELECT id, product_id, blank_model_id, height_mm FROM item WHERE product_id = ? ORDER BY height_mm ASC";
        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapItem(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Item insert(int productId, int blankModelId, double heightMm) {
        String sql = "INSERT INTO item (product_id, blank_model_id, height_mm) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setInt(2, blankModelId);
            ps.setDouble(3, heightMm);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Item(keys.getInt(1), productId, blankModelId, heightMm);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        return new Item(
                rs.getInt("id"),
                rs.getInt("product_id"),
                rs.getInt("blank_model_id"),
                rs.getDouble("height_mm")
        );
    }
}
