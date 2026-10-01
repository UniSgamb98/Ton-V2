package com.orodent.tonv2.core.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeMirrorPageBuilderTest {
    @Test
    void buildsHtmlEditorWithItsLanguageDependencies() {
        String page = CodeMirrorPageBuilder.build(CodeEditorLanguage.HTML);

        assertTrue(page.contains("mode: \"htmlmixed\""));
        assertTrue(page.contains("CodeMirror.fromTextArea"));
        assertFalse(page.contains("{{CODE_MIRROR"));
        assertFalse(page.contains("{{MODE_SCRIPTS}}"));
    }

    @Test
    void buildsSqlEditorUsingSqlMode() {
        String page = CodeMirrorPageBuilder.build(CodeEditorLanguage.SQL);

        assertTrue(page.contains("mode: \"text/x-sql\""));
    }
}
