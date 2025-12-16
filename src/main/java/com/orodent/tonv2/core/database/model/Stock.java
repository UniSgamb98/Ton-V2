package com.orodent.tonv2.core.database.model;

public record Stock(
        int id,
        int lotId,
        int depotId,
        int quantity
) {}
