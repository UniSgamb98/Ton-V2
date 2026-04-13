package com.orodent.tonv2.core.database.model;

import java.time.LocalDateTime;

public record FiringProgram(int id,
                            String name,
                            LocalDateTime createdAt) {
}
