package com.orodent.tonv2.features.cubage.creation.view;

import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import javafx.scene.control.ListCell;

public class PayloadOptionListCell extends ListCell<CubageCreationService.PayloadOption> {

    @Override
    protected void updateItem(CubageCreationService.PayloadOption item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            return;
        }
        setText(item.displayName());
    }
}
