package com.orodent.tonv2.core.database.model;

import java.time.Instant;

public record DocumentTemplate(int id,
                               String name,
                               String templateContent,
                               String sqlQuery,
                               String presetCode,
                               Instant updatedAt) {
}
