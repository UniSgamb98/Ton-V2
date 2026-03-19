package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.ProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductRepositoryImpl implements ProductRepository {
    private final Connection conn;

    public ProductRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Product> findAll() {

        String sql = """
        SELECT id, code, description
        FROM product
        ORDER BY code
        """;

        List<Product> products = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("description")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading products", e);
        }

        return products;
    }

    @Override
    public Product findById(int id) {
        String sql = """
        SELECT id, code, description
        FROM product
        WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("description")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading product with id " + id, e);
        }

        return null;
    }

    @Override
    public Product insert(String code, String description) {
        String sql = """
        INSERT INTO product (code, description)
        VALUES (?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, description);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Product(rs.getInt(1), code, description);
                }
            }

            throw new SQLException("No ID returned for product insert");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting product", e);
        }
    }

}
