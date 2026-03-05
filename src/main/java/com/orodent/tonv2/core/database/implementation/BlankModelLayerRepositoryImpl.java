package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BlankModelLayerRepositoryImpl implements BlankModelLayerRepository {
    private final Connection conn;

    public BlankModelLayerRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insert(BlankModelLayer layer) {
        String sql = """
        INSERT INTO blank_model_layer (blank_model_id, layer_number, disk_percentage)
        VALUES (?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, layer.blankModelId());
            ps.setInt(2, layer.layerNumber());
            ps.setDouble(3, layer.diskPercentage());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting blank model layer", e);
        }
    }
}
