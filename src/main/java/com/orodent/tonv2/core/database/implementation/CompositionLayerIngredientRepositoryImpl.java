package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<CompositionLayerIngredient> findByLayerId(int layerId) {
        String sql = """
        SELECT id, layer_id, powder_id, percentage
        FROM composition_layer_ingredient
        WHERE layer_id = ?
        ORDER BY id ASC
        """;

        List<CompositionLayerIngredient> ingredients = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, layerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new CompositionLayerIngredient(
                            rs.getInt("id"),
                            rs.getInt("layer_id"),
                            rs.getInt("powder_id"),
                            rs.getDouble("percentage")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding composition layer ingredients for layer " + layerId, e);
        }

        return ingredients;
    }

}
