package com.orodent.tonv2.app;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.scene.Parent;

import java.util.Objects;

/** A fully assembled page and its navigation lifecycle callbacks. */
public record AppPage(Parent root,
                      AppHeader header,
                      String title,
                      Runnable onShown,
                      Runnable onDispose) {
    public AppPage {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(header, "header");
        title = title == null ? "TON" : title;
        onShown = onShown == null ? () -> {} : onShown;
        onDispose = onDispose == null ? () -> {} : onDispose;
    }

    public static AppPage immediate(Parent root, AppHeader header, String title) {
        return new AppPage(root, header, title, null, null);
    }
}
