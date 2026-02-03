package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> findAll();
}
