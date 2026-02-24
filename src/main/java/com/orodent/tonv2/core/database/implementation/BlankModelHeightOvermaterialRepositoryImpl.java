package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BlankModelHeightOvermaterialRepositoryImpl implements BlankModelHeightOvermaterialRepository {

    private final Connection conn;

    public BlankModelHeightOvermaterialRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public int insert(BlankModelHeightOvermaterial range) {
        String sql = """
                INSERT INTO blank_model_height_overmaterial (
                    blank_model_id,
                    min_height_mm,
                    max_height_mm,
                    superior_overmaterial_mm,
                    inferior_overmaterial_mm
                ) VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, range.blankModelId());
            ps.setDouble(2, range.minHeightMm());
            ps.setDouble(3, range.maxHeightMm());
            ps.setDouble(4, range.superiorOvermaterialMm());
            ps.setDouble(5, range.inferiorOvermaterialMm());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new SQLException("No ID returned for blank_model_height_overmaterial insert");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting blank model overmaterial range", e);
        }
    }
}

