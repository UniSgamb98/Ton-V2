package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.sql.Connection;

public class ProductionRepositoryImpl implements ProductionRepository {
    private final Connection conn;

    public ProductionRepositoryImpl(Connection conn) {
        this.conn = conn;
    }
}
