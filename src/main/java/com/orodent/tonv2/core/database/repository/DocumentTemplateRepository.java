package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.DocumentTemplate;

import java.util.List;

public interface DocumentTemplateRepository {
    void ensureTable();
    void upsert(String name, String templateContent, String sqlQuery, String presetCode);
    List<DocumentTemplate> findAll();
    List<DocumentTemplate> findByNameContaining(String nameFilter);
    DocumentTemplate findById(int id);
    void updateById(int id, String name, String templateContent, String sqlQuery, String presetCode);
}
