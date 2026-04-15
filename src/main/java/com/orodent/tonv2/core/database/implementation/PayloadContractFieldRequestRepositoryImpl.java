package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.repository.PayloadContractFieldRequestRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PayloadContractFieldRequestRepositoryImpl implements PayloadContractFieldRequestRepository {

    private final Connection conn;

    public PayloadContractFieldRequestRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<String> findRequestedFieldKeysByPayloadContractId(int payloadContractId) {
        String sql = """
                SELECT pcf.field_key
                FROM payload_contract_field_request pcfr
                JOIN payload_contract_field pcf ON pcf.id = pcfr.payload_contract_field_id
                WHERE pcfr.payload_contract_id = ?
                ORDER BY pcfr.order_index ASC, pcfr.id ASC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payloadContractId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(rs.getString("field_key"));
                }
                return values;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento field request del payload contract.", e);
        }
    }
}
