package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.PayloadContractField;

import java.util.List;

public interface PayloadContractFieldRepository {
    List<PayloadContractField> findByPayloadContractId(int payloadContractId);
}
