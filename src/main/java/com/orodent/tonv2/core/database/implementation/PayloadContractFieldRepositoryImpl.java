package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.PayloadContractField;
import com.orodent.tonv2.core.database.repository.PayloadContractFieldRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PayloadContractFieldRepositoryImpl implements PayloadContractFieldRepository {

    private final Connection conn;

    public PayloadContractFieldRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<PayloadContractField> findByPayloadContractId(int payloadContractId) {
        String sql = """
                SELECT id, payload_contract_id, field_key, display_name, data_type, unit_code, order_index
                FROM payload_contract_field
                WHERE payload_contract_id = ?
                ORDER BY order_index ASC, id ASC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payloadContractId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PayloadContractField> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(new PayloadContractField(
                            rs.getInt("id"),
                            rs.getInt("payload_contract_id"),
                            rs.getString("field_key"),
                            rs.getString("display_name"),
                            rs.getString("data_type"),
                            rs.getString("unit_code"),
                            rs.getInt("order_index")
                    ));
                }
                return values;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento campi payload contract.", e);
        }
    }
}
