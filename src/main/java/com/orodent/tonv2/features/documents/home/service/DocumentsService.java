package com.orodent.tonv2.features.documents.home.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DocumentsService {
    private final List<String> documentRegistry;

    public DocumentsService() {
        this.documentRegistry = new ArrayList<>();
    }

    public String normalizeDocumentCode(String rawCode) {
        return rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
    }

    public boolean registerDocument(String rawCode) {
        String normalizedCode = normalizeDocumentCode(rawCode);
        if (normalizedCode.isBlank() || documentRegistry.contains(normalizedCode)) {
            return false;
        }

        documentRegistry.add(normalizedCode);
        return true;
    }

    public List<String> getRegisteredDocuments() {
        return Collections.unmodifiableList(documentRegistry);
    }

    public LocalDate suggestProcessingDate() {
        return LocalDate.now().plusDays(1);
    }
}
