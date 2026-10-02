package com.orodent.tonv2.features.laboratory.home.view;

import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

final class LaboratoryIcons {
    private static final String ICON_RESOURCE = "/icons/laboratory-icons.properties";
    private static final Properties ICONS = loadIcons();

    private LaboratoryIcons() {
    }

    static SVGPath create(String name) {
        String content = ICONS.getProperty(name);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Icona laboratorio non trovata: " + name);
        }

        SVGPath path = new SVGPath();
        path.setContent(content);
        return path;
    }

    private static Properties loadIcons() {
        Properties icons = new Properties();
        try (InputStream stream = Objects.requireNonNull(
                LaboratoryIcons.class.getResourceAsStream(ICON_RESOURCE),
                "Risorsa icone laboratorio non trovata: " + ICON_RESOURCE
        )) {
            icons.load(stream);
            return icons;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile caricare le icone del laboratorio", exception);
        }
    }
}
