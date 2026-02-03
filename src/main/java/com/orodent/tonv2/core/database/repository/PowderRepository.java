package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Powder;

import java.util.List;

public interface PowderRepository {

    List<Powder> findAll();
    Powder findById(int id);
    Powder save(Powder powder);
    boolean delete(int id);
}
