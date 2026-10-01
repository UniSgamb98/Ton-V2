package com.orodent.tonv2.features.documents.template.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateEditorWorkflowServiceTest {

    @Test
    void blankQueryDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        ConnectionProvider provider = () -> {
            connectionOpened.set(true);
            throw new AssertionError("A blank query must not open a connection");
        };
        TemplateEditorWorkflowService service = new TemplateEditorWorkflowService(
                new TemplateEditorService(provider),
                provider,
                new BatchProductionDocumentParamsService(provider),
                new PresinteringDocumentParamsService(provider)
        );

        assertThrows(IllegalArgumentException.class, () -> service.fetchQueryPayload("  "));
        assertFalse(connectionOpened.get());
    }
}
