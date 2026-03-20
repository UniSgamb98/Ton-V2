package com.orodent.tonv2.core.documents.template;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentDesktopService {

    public Path openHtmlPreview(String html) throws IOException {
        ensureBrowseSupported();

        Path tempFile = Files.createTempFile("ton-document-preview-", ".html");
        Files.writeString(tempFile, html, StandardCharsets.UTF_8);
        Desktop.getDesktop().browse(tempFile.toUri());
        return tempFile;
    }

    public void openDocument(Path path) throws IOException {
        ensureBrowseSupported();
        Desktop.getDesktop().browse(path.toUri());
    }

    public void printDocument(Path path) throws IOException {
        ensurePrintSupported();
        Desktop.getDesktop().print(path.toFile());
    }

    private void ensureBrowseSupported() {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IllegalStateException("Anteprima non disponibile: apertura browser non supportata in questo ambiente.");
        }
    }

    private void ensurePrintSupported() {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
            throw new IllegalStateException("Stampa non disponibile: print job non supportato in questo ambiente.");
        }
    }
}
