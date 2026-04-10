package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Firing;
import com.orodent.tonv2.core.database.repository.FiringRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class FiringRepositoryImpl implements FiringRepository {

    private final Connection conn;

    public FiringRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Firing insert(LocalDate firingDate, String furnace, Integer maxTemperature, String notes) {
        String sql = """
                INSERT INTO firing (firing_date, furnace, max_temperature, notes)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(firingDate));
            ps.setString(2, furnace);
            if (maxTemperature == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, maxTemperature);
            }
            ps.setString(4, notes);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Firing(keys.getInt(1), firingDate, furnace, maxTemperature, notes);
                }
            }
            throw new SQLException("Nessun ID restituito per firing");
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento firing", e);
        }
    }

    @Override
    public Integer findLatestId() {
        String sql = "SELECT MAX(id) AS latest_id FROM firing";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            int latestId = rs.getInt("latest_id");
            return rs.wasNull() ? null : latestId;
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento ultimo firing id.", e);
        }
    }
}
