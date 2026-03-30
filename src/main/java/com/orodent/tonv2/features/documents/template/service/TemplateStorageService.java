package com.orodent.tonv2.features.documents.template.service;

import com.orodent.tonv2.core.database.model.DocumentTemplate;
import com.orodent.tonv2.core.database.repository.DocumentTemplateRepository;

import java.time.Instant;
import java.util.List;

public class TemplateStorageService {

    private final DocumentTemplateRepository templateRepository;

    public TemplateStorageService(DocumentTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.templateRepository.ensureTable();
    }

    public void saveTemplate(String name, String templateText, String sqlQuery, String presetCode) {
        templateRepository.upsert(name, templateText, sqlQuery, presetCode);
    }

    public void updateTemplateById(int id, String name, String templateText, String sqlQuery, String presetCode) {
        templateRepository.updateById(id, name, templateText, sqlQuery, presetCode);
    }

    public List<TemplateSnapshot> loadTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toSnapshot)
                .toList();
    }

    public List<TemplateSnapshot> loadTemplatesByName(String nameFilter) {
        return templateRepository.findByNameContaining(nameFilter).stream()
                .map(this::toSnapshot)
                .toList();
    }

    public TemplateSnapshot loadTemplateById(int id) {
        DocumentTemplate template = templateRepository.findById(id);
        return template == null ? null : toSnapshot(template);
    }

    private TemplateSnapshot toSnapshot(DocumentTemplate template) {
        return new TemplateSnapshot(
                template.id(),
                template.name(),
                template.templateContent(),
                template.sqlQuery(),
                template.presetCode(),
                template.updatedAt()
        );
    }

    public record TemplateSnapshot(int id,
                                   String name,
                                   String templateContent,
                                   String sqlQuery,
                                   String presetCode,
                                   Instant savedAt) {
    }
}
