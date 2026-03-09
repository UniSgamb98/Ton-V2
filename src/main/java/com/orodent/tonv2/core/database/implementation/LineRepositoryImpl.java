package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.repository.LineRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LineRepositoryImpl implements LineRepository {

    private final Connection conn;

    public LineRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Line> findAll() {
        String sql = "SELECT id, name, product_id FROM line ORDER BY name ASC";
        List<Line> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Line(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("product_id")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento linee produzione.", e);
        }
    }

    @Override
    public List<Line> findByProductId(int productId) {
        String sql = "SELECT id, name, product_id FROM line WHERE product_id = ? ORDER BY name ASC";
        List<Line> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Line(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("product_id")
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento linee per prodotto.", e);
        }
    }
}
