package com.orodent.tonv2.core.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlPreviewBrowserLauncher {

    public LaunchResult openInBrowser(String htmlContent) {
        String safeHtml = htmlContent == null ? "" : htmlContent;
        try {
            Path tempFile = Files.createTempFile("ton-template-preview-", ".html");
            Files.writeString(tempFile, safeHtml, StandardCharsets.UTF_8);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tempFile.toUri());
                return new LaunchResult(true, "Anteprima aperta nel browser: " + tempFile);
            }
            return new LaunchResult(false, "Browser desktop non supportato in questo ambiente.");
        } catch (IOException ex) {
            return new LaunchResult(false, "Impossibile aprire l'anteprima nel browser: " + ex.getMessage());
        }
    }

    public record LaunchResult(boolean success, String message) {}
}
