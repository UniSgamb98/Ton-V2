package com.orodent.tonv2.features.registers.home.service;

import java.nio.file.Files;
import java.nio.file.Path;

public class RegistersDocumentService {

    private final RegistersSearchService searchService;

    public RegistersDocumentService(RegistersSearchService searchService) {
        this.searchService = searchService;
    }

    public String generateCompositionDocument(String itemCode, String lotCode) {
        RegistersSearchService.SearchResult result = searchService.search(itemCode, lotCode);
        if (!result.success()) {
            throw new IllegalArgumentException(result.compositionOutput());
        }

        return writeHtmlDocument(
                "Registro Composizione",
                "Documento composizione",
                result.compositionOutput(),
                "ton-register-composition-"
        );
    }

    public String generateFiringDocument(String itemCode, String lotCode) {
        RegistersSearchService.SearchResult result = searchService.search(itemCode, lotCode);
        if (!result.success()) {
            throw new IllegalArgumentException(result.firingOutput());
        }

        return writeHtmlDocument(
                "Registro Firing",
                "Documento firing",
                result.firingOutput(),
                "ton-register-firing-"
        );
    }

    private String writeHtmlDocument(String htmlTitle,
                                     String documentTitle,
                                     String rawContent,
                                     String filePrefix) {
        String safeContent = rawContent == null ? "" : rawContent;
        String html = """
                <!doctype html>
                <html lang=\"it\">
                <head>
                  <meta charset=\"UTF-8\" />
                  <title>%s</title>
                  <style>
                    body { font-family: Arial, sans-serif; padding: 24px; }
                    h1 { margin-top: 0; }
                    pre {
                      background: #f7f7f7;
                      border: 1px solid #ddd;
                      border-radius: 8px;
                      padding: 16px;
                      white-space: pre-wrap;
                    }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  <pre>%s</pre>
                </body>
                </html>
                """.formatted(htmlTitle, documentTitle, escapeHtml(safeContent));

        try {
            Path outputFile = Files.createTempFile(filePrefix, ".html");
            Files.writeString(outputFile, html);
            return outputFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Documento generato ma non salvabile su file temporaneo.");
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
