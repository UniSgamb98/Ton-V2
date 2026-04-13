package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.FiringProgram;
import com.orodent.tonv2.core.database.model.FiringProgramStep;
import com.orodent.tonv2.core.database.repository.FiringProgramRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FiringProgramRepositoryImpl implements FiringProgramRepository {

    private final Connection conn;

    public FiringProgramRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void ensureTables() {
        String firingProgramSql = """
                CREATE TABLE firing_program (
                    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String firingProgramStepSql = """
                CREATE TABLE firing_program_step (
                    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    firing_program_id INT NOT NULL,
                    step_order INT NOT NULL,
                    target_temperature DOUBLE NOT NULL,
                    ramp_time_minutes INT NOT NULL,
                    hold_time_minutes INT NOT NULL,
                    CONSTRAINT fk_firing_program_step_program
                        FOREIGN KEY (firing_program_id) REFERENCES firing_program(id) ON DELETE CASCADE
                )
                """;

        createTableIfMissing(firingProgramSql);
        createTableIfMissing(firingProgramStepSql);
    }

    private void createTableIfMissing(String sql) {
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (!"X0Y32".equals(sqlState)) {
                throw new RuntimeException("Errore creazione tabelle firing program.", e);
            }
        }
    }

    @Override
    public FiringProgram insertProgram(String name) {
        String sql = "INSERT INTO firing_program (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Nessun ID restituito per firing_program.");
                }
                int id = keys.getInt(1);
                Timestamp createdAt = readCreatedAt(id);
                return new FiringProgram(id, name, createdAt == null ? null : createdAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento firing_program.", e);
        }
    }

    private Timestamp readCreatedAt(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT created_at FROM firing_program WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("created_at") : null;
            }
        }
    }

    @Override
    public void insertStep(int firingProgramId, int stepOrder, double targetTemperature, int rampTimeMinutes, int holdTimeMinutes) {
        String sql = """
                INSERT INTO firing_program_step (firing_program_id, step_order, target_temperature, ramp_time_minutes, hold_time_minutes)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, firingProgramId);
            ps.setInt(2, stepOrder);
            ps.setDouble(3, targetTemperature);
            ps.setInt(4, rampTimeMinutes);
            ps.setInt(5, holdTimeMinutes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore inserimento firing_program_step.", e);
        }
    }

    @Override
    public List<FiringProgramStep> findStepsByProgramId(int firingProgramId) {
        String sql = """
                SELECT id, firing_program_id, step_order, target_temperature, ramp_time_minutes, hold_time_minutes
                FROM firing_program_step
                WHERE firing_program_id = ?
                ORDER BY step_order ASC
                """;

        List<FiringProgramStep> steps = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, firingProgramId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    steps.add(new FiringProgramStep(
                            rs.getInt("id"),
                            rs.getInt("firing_program_id"),
                            rs.getInt("step_order"),
                            rs.getDouble("target_temperature"),
                            rs.getInt("ramp_time_minutes"),
                            rs.getInt("hold_time_minutes")
                    ));
                }
            }
            return steps;
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento step firing_program.", e);
        }
    }
}
