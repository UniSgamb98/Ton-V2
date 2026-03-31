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
import java.util.Optional;

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
                       version,
                       diameter_mm,
                       superior_overmaterial_default_mm,
                       inferior_overmaterial_default_mm,
                       pressure_kg_cm2,
                       grams_per_mm,
                       num_layers
                FROM blank_model
                ORDER BY code ASC, version DESC
                """;

        List<BlankModel> blankModels = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                blankModels.add(new BlankModel(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getInt("version"),
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
                       version,
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
                            rs.getInt("version"),
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
    public Optional<Integer> findMaxVersionByCode(String code) {
        String sql = "SELECT MAX(version) FROM blank_model WHERE UPPER(code) = UPPER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int max = rs.getInt(1);
                    return rs.wasNull() ? Optional.empty() : Optional.of(max);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding max blank model version for code " + code, e);
        }
    }

    @Override
    public BlankModel insert(String code, int version, double diameterMm, double superiorOvermaterialDefaultMm, double inferiorOvermaterialDefaultMm, double pressureKgCm2, double gramsPerMm, int numLayers) {
        String sql = """
                INSERT INTO blank_model (
                    code,
                    version,
                    diameter_mm,
                    superior_overmaterial_default_mm,
                    inferior_overmaterial_default_mm,
                    pressure_kg_cm2,
                    grams_per_mm,
                    num_layers
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setInt(2, version);
            ps.setDouble(3, diameterMm);
            ps.setDouble(4, superiorOvermaterialDefaultMm);
            ps.setDouble(5, inferiorOvermaterialDefaultMm);
            ps.setDouble(6, pressureKgCm2);
            ps.setDouble(7, gramsPerMm);
            ps.setInt(8, numLayers);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new BlankModel(rs.getInt(1), code, version, diameterMm, superiorOvermaterialDefaultMm, inferiorOvermaterialDefaultMm, pressureKgCm2, gramsPerMm, numLayers);
                }
            }

            throw new SQLException("No ID returned for blank model insert");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting blank model", e);
        }
    }
}
