package com.orodent.tonv2.core.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaScriptStringEncoderTest {
    @Test
    void quotesTextWithoutChangingPrintableContent() {
        assertEquals("\"line 1\\n\\\"quoted\\\" \\\\ path 'single'\"",
                JavaScriptStringEncoder.quote("line 1\n\"quoted\" \\ path 'single'"));
    }

    @Test
    void escapesJavaScriptLineSeparatorsAndControlCharacters() {
        assertEquals("\"a\\u2028b\\u2029c\\u0001\"",
                JavaScriptStringEncoder.quote("a\u2028b\u2029c\u0001"));
    }

    @Test
    void treatsNullAsEmptyText() {
        assertEquals("\"\"", JavaScriptStringEncoder.quote(null));
    }
}
