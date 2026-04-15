package com.orodent.tonv2.core.database.model;

public record PayloadContract(
        int id,
        String contractCode,
        int version
) {}
