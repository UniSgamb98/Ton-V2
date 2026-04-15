package com.orodent.tonv2.core.database.repository;

import java.util.List;

public interface PayloadContractFieldRequestRepository {
    List<String> findRequestedFieldKeysByPayloadContractId(int payloadContractId);
}
