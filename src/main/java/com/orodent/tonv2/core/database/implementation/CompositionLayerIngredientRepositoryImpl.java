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
            composition_id,
            layer_number,
            powder_id,
            percentage
        ) VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cli.compositionId());
            ps.setInt(2, cli.layerNumber());
            ps.setInt(3, cli.powderId());
            ps.setDouble(4, cli.percentage());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting composition layer ingredient", e);
        }
    }

    @Override
    public List<CompositionLayerIngredient> findByCompositionId(int compositionId) {
        String sql = """
        SELECT cli.composition_id, cli.layer_number, cli.powder_id, cli.percentage
        FROM composition_layer_ingredient cli
        JOIN powder p ON p.id = cli.powder_id
        WHERE cli.composition_id = ?
        ORDER BY cli.layer_number ASC, p.view_order ASC, p.yttria ASC, cli.powder_id ASC
        """;

        List<CompositionLayerIngredient> ingredients = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, compositionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new CompositionLayerIngredient(
                            rs.getInt("composition_id"),
                            rs.getInt("layer_number"),
                            rs.getInt("powder_id"),
                            rs.getDouble("percentage")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding composition layer ingredients for composition " + compositionId, e);
        }

        return ingredients;
    }

}
