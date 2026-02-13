package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.CompositionLayer;
import com.orodent.tonv2.core.database.repository.CompositionLayerRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompositionLayerRepositoryImpl implements CompositionLayerRepository {
    private final Connection conn;

    public CompositionLayerRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public int insert(CompositionLayer layer) {

        String sql = """
        INSERT INTO composition_layer (
            composition_id,
            layer_number,
            notes
        ) VALUES (?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, layer.compositionId());
            ps.setInt(2, layer.layerNumber());
            ps.setString(3, layer.notes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("No ID returned for composition_layer insert");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting composition layer", e);
        }
    }


    @Override
    public List<CompositionLayer> findByCompositionId(int compositionId) {

        String sql = """
        SELECT id, composition_id, layer_number, notes
        FROM composition_layer
        WHERE composition_id = ?
        ORDER BY layer_number ASC
        """;

        List<CompositionLayer> layers = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, compositionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    layers.add(new CompositionLayer(
                            rs.getInt("id"),
                            rs.getInt("composition_id"),
                            rs.getInt("layer_number"),
                            rs.getString("notes")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding composition layers for composition " + compositionId, e);
        }

        return layers;
    }

}
