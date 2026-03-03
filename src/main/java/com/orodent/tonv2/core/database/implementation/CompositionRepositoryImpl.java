package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.repository.CompositionRepository;

import java.sql.*;
import java.util.Optional;

public class CompositionRepositoryImpl implements CompositionRepository {
    private final Connection conn;

    public CompositionRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Optional<Integer> findMaxVersionByProduct(int productId) {

        String sql = """
        SELECT MAX(version)
        FROM composition
        WHERE product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int max = rs.getInt(1);
                    return rs.wasNull()
                            ? Optional.empty()
                            : Optional.of(max);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error finding max composition version for product " + productId, e
            );
        }

        return Optional.empty();
    }


    @Override
    public Optional<Composition> findLatestByProduct(int productId) {

        String sql = """
        SELECT id, product_id, version, num_layers, created_at, notes
        FROM composition
        WHERE product_id = ?
        ORDER BY version DESC
        FETCH FIRST 1 ROW ONLY
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return Optional.of(new Composition(
                            rs.getInt("id"),
                            rs.getInt("product_id"),
                            rs.getInt("version"),
                            rs.getInt("num_layers"),
                            createdAt != null ? createdAt.toLocalDateTime() : null,
                            rs.getString("notes")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error finding latest composition for product " + productId, e
            );
        }

        return Optional.empty();
    }

    @Override
    public Optional<Integer> findActiveCompositionId(int productId) {
        String sql = "SELECT composition_id FROM product_active_composition WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("composition_id"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error reading active composition for product " + productId, e);
        }
    }

    @Override
    public void setActiveComposition(int productId, int compositionId) {
        String updateSql = "UPDATE product_active_composition SET composition_id = ? WHERE product_id = ?";
        String insertSql = "INSERT INTO product_active_composition (product_id, composition_id) VALUES (?, ?)";

        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            updatePs.setInt(1, compositionId);
            updatePs.setInt(2, productId);
            int updated = updatePs.executeUpdate();

            if (updated == 0) {
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, productId);
                    insertPs.setInt(2, compositionId);
                    insertPs.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error setting active composition", e);
        }
    }

    @Override
    public int insert(Composition composition) {
        String sql = """
        INSERT INTO composition (
            product_id,
            version,
            num_layers,
            created_at,
            notes
        ) VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, composition.productId());
            ps.setInt(2, composition.version());
            ps.setInt(3, composition.numLayers());
            ps.setTimestamp(4, Timestamp.valueOf(composition.createdAt()));
            ps.setString(5, composition.notes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("No ID returned for composition insert");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting composition", e);
        }
    }
}
