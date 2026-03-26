package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BlankModelRepositoryImpl implements BlankModelRepository {

    private final Connection conn;

    public BlankModelRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<BlankModel> findAll() {

        String sql = """
                SELECT id,
                       code,
                       diameter_mm,
                       superior_overmaterial_default_mm,
                       inferior_overmaterial_default_mm,
                       pressure_kg_cm2,
                       grams_per_mm,
                       num_layers
                FROM blank_model
                ORDER BY code
                """;

        List<BlankModel> blankModels = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                blankModels.add(new BlankModel(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getDouble("diameter_mm"),
                        rs.getDouble("superior_overmaterial_default_mm"),
                        rs.getDouble("inferior_overmaterial_default_mm"),
                        rs.getDouble("pressure_kg_cm2"),
                        rs.getDouble("grams_per_mm"),
                        rs.getInt("num_layers")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading blank models", e);
        }

        return blankModels;
    }

    @Override
    public BlankModel findById(int id) {

        String sql = """
                SELECT id,
                       code,
                       diameter_mm,
                       superior_overmaterial_default_mm,
                       inferior_overmaterial_default_mm,
                       pressure_kg_cm2,
                       grams_per_mm,
                       num_layers
                FROM blank_model
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BlankModel(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getDouble("diameter_mm"),
                            rs.getDouble("superior_overmaterial_default_mm"),
                            rs.getDouble("inferior_overmaterial_default_mm"),
                            rs.getDouble("pressure_kg_cm2"),
                            rs.getDouble("grams_per_mm"),
                            rs.getInt("num_layers")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading blank model with id " + id, e);
        }

        return null;
    }

    @Override
    public BlankModel insert(String code, double diameterMm, double superiorOvermaterialDefaultMm, double inferiorOvermaterialDefaultMm, double pressureKgCm2, double gramsPerMm, int numLayers) {
        String sql = """
                INSERT INTO blank_model (
                    code,
                    diameter_mm,
                    superior_overmaterial_default_mm,
                    inferior_overmaterial_default_mm,
                    pressure_kg_cm2,
                    grams_per_mm,
                    num_layers
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setDouble(2, diameterMm);
            ps.setDouble(3, superiorOvermaterialDefaultMm);
            ps.setDouble(4, inferiorOvermaterialDefaultMm);
            ps.setDouble(5, pressureKgCm2);
            ps.setDouble(6, gramsPerMm);
            ps.setInt(7, numLayers);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new BlankModel(rs.getInt(1), code, diameterMm, superiorOvermaterialDefaultMm, inferiorOvermaterialDefaultMm, pressureKgCm2, gramsPerMm, numLayers);
                }
            }

            throw new SQLException("No ID returned for blank model insert");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting blank model", e);
        }
    }
}
