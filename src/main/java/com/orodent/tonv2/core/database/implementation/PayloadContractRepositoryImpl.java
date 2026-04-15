package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.PayloadContract;
import com.orodent.tonv2.core.database.repository.PayloadContractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PayloadContractRepositoryImpl implements PayloadContractRepository {

    private final Connection conn;

    public PayloadContractRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<PayloadContract> findAll() {
        String sql = """
                SELECT id, contract_code, version
                FROM payload_contract
                ORDER BY contract_code ASC, version ASC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PayloadContract> values = new ArrayList<>();
            while (rs.next()) {
                values.add(toPayloadContract(rs));
            }
            return values;
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento payload contract.", e);
        }
    }

    @Override
    public List<PayloadContract> findByContractCode(String contractCode) {
        String sql = """
                SELECT id, contract_code, version
                FROM payload_contract
                WHERE contract_code = ?
                ORDER BY version ASC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contractCode);
            try (ResultSet rs = ps.executeQuery()) {
                List<PayloadContract> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(toPayloadContract(rs));
                }
                return values;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento versioni payload contract.", e);
        }
    }

    private PayloadContract toPayloadContract(ResultSet rs) throws SQLException {
        return new PayloadContract(
                rs.getInt("id"),
                rs.getString("contract_code"),
                rs.getInt("version")
        );
    }
}
