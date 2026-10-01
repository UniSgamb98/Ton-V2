package com.orodent.tonv2.features.cubage.creation.service;

import com.orodent.tonv2.core.database.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CubageFormulaSetPersistenceService {

    private final ConnectionProvider connectionProvider;

    public CubageFormulaSetPersistenceService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public SaveResult save(CubageCreationService.FormulaCompilation compilation) {
        if (compilation == null) {
            throw new IllegalArgumentException("Compilazione formule assente.");
        }
        if (!compilation.missingRequested().isEmpty()) {
            throw new IllegalArgumentException("Impossibile salvare: output richiesti mancanti.");
        }

        return connectionProvider.withTransaction(connection -> {
            int nextVersion = findNextVersionByCode(connection, compilation.formulaSetName());
            int formulaSetId = insertFormulaSet(connection, compilation.formulaSetName(), nextVersion);

            linkFormulaSetToPayload(connection, formulaSetId, compilation.selectedPayload().payloadContractId());

            int orderIndex = 0;
            for (CubageCreationService.FormulaDefinition formula : compilation.formulas()) {
                int formulaId = insertFormula(connection, formulaSetId, formula.variable(), formula.expression(), orderIndex++);
                for (String inputFieldKey : formula.inputDependencies()) {
                    insertFormulaInput(connection, formulaId, inputFieldKey);
                }
            }
            return new SaveResult(formulaSetId, nextVersion);
        });
    }

    public List<String> loadFormulaSetCodes() {
        String sql = "SELECT DISTINCT code FROM formula_set ORDER BY code ASC";
        return connectionProvider.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(rs.getString("code"));
                }
                return values;
            }
        });
    }

    private int findNextVersionByCode(Connection connection, String code) throws SQLException {
        String sql = "SELECT COALESCE(MAX(version), 0) + 1 AS next_version FROM formula_set WHERE code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("next_version");
                }
                return 1;
            }
        }
    }

    private int insertFormulaSet(Connection connection, String code, int version) throws SQLException {
        String sql = "INSERT INTO formula_set (code, version) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setInt(2, version);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Nessun ID generato per formula_set.");
            }
        }
    }

    private void linkFormulaSetToPayload(Connection connection, int formulaSetId, int payloadContractId) throws SQLException {
        String sql = "INSERT INTO formula_set_payload_contract (formula_set_id, payload_contract_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formulaSetId);
            ps.setInt(2, payloadContractId);
            ps.executeUpdate();
        }
    }

    private int insertFormula(Connection connection, int formulaSetId, String formulaKey, String expression, int orderIndex) throws SQLException {
        String sql = "INSERT INTO formula_set_formula (formula_set_id, formula_key, formula_expression, order_index) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, formulaSetId);
            ps.setString(2, formulaKey);
            ps.setString(3, expression);
            ps.setInt(4, orderIndex);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Nessun ID generato per formula_set_formula.");
            }
        }
    }

    private void insertFormulaInput(Connection connection, int formulaId, String fieldKey) throws SQLException {
        String sql = "INSERT INTO formula_set_formula_input (formula_id, field_key) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formulaId);
            ps.setString(2, fieldKey);
            ps.executeUpdate();
        }
    }

    public record SaveResult(int formulaSetId, int version) {
    }
}
