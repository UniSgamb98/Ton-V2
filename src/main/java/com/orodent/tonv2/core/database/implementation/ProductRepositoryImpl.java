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
        SELECT id, type, color
        FROM product
        ORDER BY type, color
        """;

        List<Product> products = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("color")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading products", e);
        }

        return products;
    }

}
