package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BlankModelLayerRepositoryImpl implements BlankModelLayerRepository {

    private final Connection conn;

    public BlankModelLayerRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public int insert(BlankModelLayer layer) {
        String sql = """
                INSERT INTO blank_model_layer (
                    blank_model_id,
                    layer_number,
                    occupied_space_percent
                ) VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, layer.blankModelId());
            ps.setInt(2, layer.layerNumber());
            ps.setDouble(3, layer.occupiedSpacePercent());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new SQLException("No ID returned for blank_model_layer insert");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting blank model layer", e);
        }
    }
}
