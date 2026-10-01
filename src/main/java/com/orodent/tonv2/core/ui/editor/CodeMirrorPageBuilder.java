package com.orodent.tonv2.core.ui.editor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Builds the self-contained editor page from application resources. */
final class CodeMirrorPageBuilder {
    private static final String PAGE_TEMPLATE = load("/codemirror/editor.html");
    private static final String CODE_MIRROR_CSS = load("/codemirror/codemirror.min.css");
    private static final String CODE_MIRROR_JS = load("/codemirror/codemirror.min.js");

    private CodeMirrorPageBuilder() {
    }

    static String build(CodeEditorLanguage language) {
        StringBuilder modeScripts = new StringBuilder();
        for (String resource : language.modeResources()) {
            modeScripts.append("<script>")
                    .append(load(resource))
                    .append("</script>\n");
        }
        return PAGE_TEMPLATE
                .replace("{{CODE_MIRROR_CSS}}", CODE_MIRROR_CSS)
                .replace("{{CODE_MIRROR_JS}}", CODE_MIRROR_JS)
                .replace("{{MODE_SCRIPTS}}", modeScripts)
                .replace("{{EDITOR_MODE}}", JavaScriptStringEncoder.quote(language.mode()));
    }

    private static String load(String path) {
        try (InputStream stream = CodeMirrorPageBuilder.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Risorsa editor non trovata: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile caricare la risorsa editor: " + path, exception);
        }
    }
}
