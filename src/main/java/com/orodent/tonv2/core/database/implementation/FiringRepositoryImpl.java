package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.repository.FiringRepository;

import java.sql.Connection;

public class FiringRepositoryImpl implements FiringRepository {

    private final Connection conn;

    public FiringRepositoryImpl(Connection conn) {
        this.conn = conn;
    }
}
