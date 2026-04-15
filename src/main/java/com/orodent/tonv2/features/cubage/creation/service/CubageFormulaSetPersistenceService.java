package com.orodent.tonv2.features.cubage.creation.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CubageFormulaSetPersistenceService {

    private final Connection connection;

    public CubageFormulaSetPersistenceService(Connection connection) {
        this.connection = connection;
    }

    public SaveResult save(CubageCreationService.FormulaCompilation compilation) {
        if (compilation == null) {
            throw new IllegalArgumentException("Compilazione formule assente.");
        }

        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("Errore inizializzazione transazione salvataggio formule.", e);
        }

        try {
            int nextVersion = findNextVersionByCode(compilation.formulaSetName());
            int formulaSetId = insertFormulaSet(compilation.formulaSetName(), nextVersion);

            linkFormulaSetToPayload(formulaSetId, compilation.selectedPayload().payloadContractId());

            int orderIndex = 0;
            for (CubageCreationService.FormulaDefinition formula : compilation.formulas()) {
                int formulaId = insertFormula(formulaSetId, formula.variable(), formula.expression(), orderIndex++);
                for (String inputFieldKey : formula.inputDependencies()) {
                    insertFormulaInput(formulaId, inputFieldKey);
                }
            }

            connection.commit();
            connection.setAutoCommit(originalAutoCommit);

            return new SaveResult(formulaSetId, nextVersion);
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            throw new RuntimeException("Errore salvataggio set di calcolo.", e);
        }
    }

    private int findNextVersionByCode(String code) throws SQLException {
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

    private int insertFormulaSet(String code, int version) throws SQLException {
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

    private void linkFormulaSetToPayload(int formulaSetId, int payloadContractId) throws SQLException {
        String sql = "INSERT INTO formula_set_payload_contract (formula_set_id, payload_contract_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formulaSetId);
            ps.setInt(2, payloadContractId);
            ps.executeUpdate();
        }
    }

    private int insertFormula(int formulaSetId, String formulaKey, String expression, int orderIndex) throws SQLException {
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

    private void insertFormulaInput(int formulaId, String fieldKey) throws SQLException {
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
