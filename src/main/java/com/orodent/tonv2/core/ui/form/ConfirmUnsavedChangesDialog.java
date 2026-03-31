package com.orodent.tonv2.core.ui.form;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public final class ConfirmUnsavedChangesDialog {

    private ConfirmUnsavedChangesDialog() {
    }

    public enum UserChoice {
        SAVE,
        DISCARD,
        CANCEL
    }

    public static UserChoice show(String title,
                                  String header,
                                  String content,
                                  String saveButtonLabel) {
        ButtonType save = new ButtonType(saveButtonLabel, ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("Non salvare", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(save, discard, cancel);

        ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == save) {
            return UserChoice.SAVE;
        }
        if (result == discard) {
            return UserChoice.DISCARD;
        }
        return UserChoice.CANCEL;
    }
}
