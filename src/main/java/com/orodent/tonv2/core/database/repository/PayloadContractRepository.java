package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.PayloadContract;

import java.util.List;

public interface PayloadContractRepository {
    List<PayloadContract> findAll();
    List<PayloadContract> findByContractCode(String contractCode);
}
