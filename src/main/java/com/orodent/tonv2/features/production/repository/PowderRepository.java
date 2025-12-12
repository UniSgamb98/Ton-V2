package com.orodent.tonv2.features.production.repository;

import com.orodent.tonv2.features.production.model.Powder;

import java.util.List;

public interface PowderRepository {

    List<Powder> findAll();
    Powder findById(int id);
    Powder save(Powder powder);
    boolean delete(int id);
}
