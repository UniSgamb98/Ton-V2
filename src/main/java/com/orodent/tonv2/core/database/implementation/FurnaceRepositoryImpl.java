package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.FurnaceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FurnaceRepositoryImpl implements FurnaceRepository {

    private final Connection conn;

    public FurnaceRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Furnace> findAll() {
        String sql = """
                SELECT id, number
                FROM furnace
                ORDER BY id ASC
                """;

        List<Furnace> furnaces = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                furnaces.add(new Furnace(
                        rs.getInt("id"),
                        rs.getString("number")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il caricamento dei forni.", e);
        }

        return furnaces;
    }
}
