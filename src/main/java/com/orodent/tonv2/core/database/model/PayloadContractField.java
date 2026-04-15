package com.orodent.tonv2.core.database.model;

public record PayloadContractField(
        int id,
        int payloadContractId,
        String fieldKey,
        String displayName,
        String dataType,
        String unitCode,
        int orderIndex,
        String fieldRole
) {}
