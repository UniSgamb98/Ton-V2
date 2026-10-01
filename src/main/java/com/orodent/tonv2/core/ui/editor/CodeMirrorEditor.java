package com.orodent.tonv2.core.ui.editor;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.Objects;

/** Reusable offline code editor backed by the bundled CodeMirror runtime. */
public final class CodeMirrorEditor extends StackPane {
    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private boolean ready;
    private String pendingValue;

    public CodeMirrorEditor(CodeEditorLanguage language, String initialValue) {
        Objects.requireNonNull(language, "language");
        pendingValue = normalize(initialValue);
        getChildren().add(webView);
        setMinHeight(220);

        engine.getLoadWorker().stateProperty().addListener((observable, previous, current) -> {
            if (current == Worker.State.SUCCEEDED) {
                ready = true;
                applyPendingValue();
            }
        });
        engine.loadContent(CodeMirrorPageBuilder.build(language));
    }

    public String getValue() {
        if (!ready) {
            return pendingValue;
        }
        Object value = engine.executeScript("window.editor ? window.editor.getValue() : ''");
        return normalize(value == null ? null : value.toString());
    }

    public void setValue(String value) {
        pendingValue = normalize(value);
        if (ready) {
            applyPendingValue();
        }
    }

    public void insertSnippet(String snippet) {
        String normalizedSnippet = normalize(snippet);
        if (!ready) {
            pendingValue += normalizedSnippet.replace("£", "");
            return;
        }
        engine.executeScript("window.insertSnippet(" + JavaScriptStringEncoder.quote(normalizedSnippet) + ");");
        focusEditor();
    }

    public void focusEditor() {
        webView.requestFocus();
        if (ready) {
            engine.executeScript("window.editor.focus();");
        }
    }

    public void focusLine(int lineNumber) {
        if (lineNumber <= 0) {
            focusEditor();
            return;
        }
        if (ready) {
            engine.executeScript("window.focusEditorLine(" + (lineNumber - 1) + ");");
        }
    }

    private void applyPendingValue() {
        engine.executeScript("window.editor.setValue(" + JavaScriptStringEncoder.quote(pendingValue) + ");");
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }
}
