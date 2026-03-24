package com.orodent.tonv2.features.documents.template.view.components;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class CodeMirrorEditor extends StackPane {

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private boolean ready;
    private String pendingValue = "";

    public CodeMirrorEditor(String mode, String initialValue) {
        getChildren().add(webView);
        setMinHeight(220);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                ready = true;
                setValue(pendingValue);
            }
        });

        pendingValue = initialValue == null ? "" : initialValue;
        engine.loadContent(buildHtml(mode));
    }

    public String getValue() {
        if (!ready) {
            return pendingValue;
        }
        Object value = engine.executeScript("window.editor ? window.editor.getValue() : ''");
        return value == null ? "" : value.toString();
    }

    public void setValue(String value) {
        pendingValue = value == null ? "" : value;
        if (!ready) {
            return;
        }
        engine.executeScript("window.editor.setValue(" + toJsString(pendingValue) + ");");
    }

    public void insertSnippet(String snippet) {
        String safeSnippet = snippet == null ? "" : snippet;
        if (!ready) {
            pendingValue = pendingValue + safeSnippet;
            return;
        }
        engine.executeScript("window.insertSnippet(" + toJsString(safeSnippet) + ");");
    }

    private String buildHtml(String mode) {
        String safeMode = mode == null || mode.isBlank() ? "text/x-freemarker" : mode;
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.17/codemirror.min.css" />
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.17/codemirror.min.js"></script>
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.17/mode/xml/xml.min.js"></script>
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.17/mode/sql/sql.min.js"></script>
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.17/mode/htmlmixed/htmlmixed.min.js"></script>
                  <style>
                    html, body { height:100%%; margin:0; background:#0f172a; }
                    .CodeMirror { height:100vh; font-size:13px; background:#0f172a; color:#e2e8f0; }
                    .CodeMirror-gutters { background:#1e293b; border-right:1px solid #334155; }
                  </style>
                </head>
                <body>
                  <textarea id="editor"></textarea>
                  <script>
                    window.editor = CodeMirror.fromTextArea(document.getElementById('editor'), {
                      lineNumbers: true,
                      mode: '%s',
                      tabSize: 2,
                      indentUnit: 2,
                      matchBrackets: true,
                      autoCloseBrackets: true
                    });
                    window.insertSnippet = function(snippet) {
                      const doc = window.editor.getDoc();
                      const cursor = doc.getCursor();
                      doc.replaceRange(snippet, cursor);
                      window.editor.focus();
                    }
                  </script>
                </body>
                </html>
                """.formatted(escapeJsSingleQuotedString(safeMode));
    }

    private String toJsString(String raw) {
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\"", "\\\"")
                .replace("'", "\\'");
        return "\"" + escaped + "\"";
    }

    private String escapeJsSingleQuotedString(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "")
                .replace("\r", "");
    }
}
