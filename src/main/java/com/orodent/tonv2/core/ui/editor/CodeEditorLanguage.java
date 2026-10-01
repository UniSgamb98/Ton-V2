package com.orodent.tonv2.core.ui.editor;

import java.util.List;

/** Languages supported by the bundled, offline code editor. */
public enum CodeEditorLanguage {
    HTML("htmlmixed", List.of(
            "/codemirror/mode/xml/xml.min.js",
            "/codemirror/mode/css/css.min.js",
            "/codemirror/mode/javascript/javascript.min.js",
            "/codemirror/mode/htmlmixed/htmlmixed.min.js"
    )),
    SQL("text/x-sql", List.of("/codemirror/mode/sql/sql.min.js"));

    private final String mode;
    private final List<String> modeResources;

    CodeEditorLanguage(String mode, List<String> modeResources) {
        this.mode = mode;
        this.modeResources = modeResources;
    }

    String mode() {
        return mode;
    }

    List<String> modeResources() {
        return modeResources;
    }
}
