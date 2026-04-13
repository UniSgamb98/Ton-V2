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
        String sql = "SELECT id, code, product_id, blank_model_id, height_mm FROM item WHERE id = ?";
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
    public Item findByCode(String code) {
        String sql = "SELECT id, code, product_id, blank_model_id, height_mm FROM item WHERE code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
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
        String sql = "SELECT id, code, product_id, blank_model_id, height_mm FROM item WHERE product_id = ? AND height_mm = ?";
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
        SELECT DISTINCT i.id, i.code, i.product_id, i.blank_model_id, i.height_mm
        FROM item i
        JOIN production_order_line_firing polf ON polf.item_id = i.id
        JOIN firing f ON f.id = polf.firing_id
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
        String sql = "SELECT id, code, product_id, blank_model_id, height_mm FROM item ORDER BY product_id ASC, height_mm ASC";

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
        String sql = "SELECT id, code, product_id, blank_model_id, height_mm FROM item WHERE product_id = ? ORDER BY height_mm ASC";
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
    public List<Item> findByCodePrefix(String codePrefix, int limit) {
        String normalizedPrefix = codePrefix == null ? "" : codePrefix.trim();
        int safeLimit = Math.max(1, limit);

        String sql = """
                SELECT id, code, product_id, blank_model_id, height_mm
                FROM item
                WHERE UPPER(code) LIKE UPPER(?)
                ORDER BY code ASC
                FETCH FIRST ? ROWS ONLY
                """;

        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedPrefix + "%");
            ps.setInt(2, safeLimit);

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
    public List<Item> findByLotCodePrefix(String lotCodePrefix, int limit) {
        String normalizedPrefix = lotCodePrefix == null ? "" : lotCodePrefix.trim();
        int safeLimit = Math.max(1, limit);

        String sql = """
                SELECT DISTINCT i.id, i.code, i.product_id, i.blank_model_id, i.height_mm
                FROM item i
                JOIN production_order_line_firing polf ON polf.item_id = i.id
                JOIN lot l ON l.firing_id = polf.firing_id
                WHERE UPPER(l.code) LIKE UPPER(?)
                ORDER BY i.code ASC
                FETCH FIRST ? ROWS ONLY
                """;

        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedPrefix + "%");
            ps.setInt(2, safeLimit);

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
    public List<Item> findByFiringId(int firingId) {
        String sql = """
                SELECT DISTINCT i.id, i.code, i.product_id, i.blank_model_id, i.height_mm
                FROM item i
                JOIN production_order_line_firing polf ON polf.item_id = i.id
                WHERE polf.firing_id = ?
                ORDER BY i.code ASC
                """;

        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, firingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapItem(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento item per firing.", e);
        }
    }


    @Override
    public List<ItemFiringQuantityRow> findItemQuantitiesByFiringId(int firingId) {
        String sql = """
                SELECT i.id AS item_id,
                       i.code AS item_code,
                       SUM(polf.quantity) AS qty
                FROM production_order_line_firing polf
                JOIN item i ON i.id = polf.item_id
                WHERE polf.firing_id = ?
                GROUP BY i.id, i.code
                ORDER BY i.code ASC
                """;

        List<ItemFiringQuantityRow> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, firingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ItemFiringQuantityRow(
                            rs.getInt("item_id"),
                            rs.getString("item_code"),
                            rs.getInt("qty")
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento quantità item per firing.", e);
        }
    }

    @Override
    public Item insert(String code, int productId, int blankModelId, double heightMm) {
        String sql = "INSERT INTO item (code, product_id, blank_model_id, height_mm) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setInt(2, productId);
            ps.setInt(3, blankModelId);
            ps.setDouble(4, heightMm);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Item(keys.getInt(1), code, productId, blankModelId, heightMm);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        return new Item(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getInt("product_id"),
                rs.getInt("blank_model_id"),
                rs.getDouble("height_mm")
        );
    }
}
