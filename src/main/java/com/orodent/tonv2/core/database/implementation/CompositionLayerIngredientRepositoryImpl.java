package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CompositionLayerIngredientRepositoryImpl implements CompositionLayerIngredientRepository {
    private final Connection conn;

    public CompositionLayerIngredientRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insert(CompositionLayerIngredient cli) {
        String sql = """
        INSERT INTO composition_layer_ingredient (
            layer_id,
            powder_id,
            percentage
        ) VALUES (?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cli.layerId());
            ps.setInt(2, cli.powderId());
            ps.setDouble(3, cli.percentage());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting composition layer ingredient", e);
        }
    }

}
