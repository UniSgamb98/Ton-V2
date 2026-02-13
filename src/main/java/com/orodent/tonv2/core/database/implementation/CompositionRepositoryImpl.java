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
    public void deactivateActiveByProduct(int itemId) {

        String sql = """
        UPDATE composition
        SET is_active = 0
        WHERE item_id = ?
          AND is_active = 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error deactivating active composition for item " + itemId, e
            );
        }
    }

    @Override
    public Optional<Integer> findMaxVersionByProduct(int itemId) {

        String sql = """
        SELECT MAX(version)
        FROM composition
        WHERE item_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);

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
                    "Error finding max composition version for item " + itemId, e
            );
        }

        return Optional.empty();
    }

    @Override
    public int insert(Composition composition) {
        String sql = """
        INSERT INTO composition (
            item_id,
            version,
            is_active,
            created_at,
            notes
        ) VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, composition.itemId());
            ps.setInt(2, composition.version());
            ps.setInt(3, composition.active() ? 1 : 0);
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
