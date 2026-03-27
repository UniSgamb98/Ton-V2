package com.orodent.tonv2.features.document.service;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

public class DocumentBrowserService {

    public void openDocument(String documentPath) {
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("Percorso documento non valido.");
        }

        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalArgumentException("Apertura browser non supportata in questo ambiente.");
            }

            URI documentUri = Path.of(documentPath).toUri();
            Desktop.getDesktop().browse(documentUri);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossibile aprire il documento nel browser.");
        }
    }
}
