package com.orodent.tonv2.features.inventory.database.repository.implementation;

import com.orodent.tonv2.features.inventory.database.model.Depot;
import com.orodent.tonv2.features.inventory.database.repository.DepotRepository;

import java.sql.*;

public class DepotRepositoryImpl implements DepotRepository {

    private final Connection conn;

    public DepotRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Depot findByName(String name) {
        String sql = "SELECT * FROM depot WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Depot(rs.getInt("id"), rs.getString("name"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Depot insert(String name) {
        String sql = "INSERT INTO depot (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new Depot(keys.getInt(1), name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
