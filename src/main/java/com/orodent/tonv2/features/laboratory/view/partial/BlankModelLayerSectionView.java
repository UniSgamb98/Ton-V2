package com.orodent.tonv2.features.laboratory.view.partial;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class BlankModelLayerSectionView extends VBox {

    private final VBox rowsBox = new VBox(8);
    private final Button addLayerBtn = new Button("Aggiungi strato");
    private final List<BlankModelLayerRowView> rows = new ArrayList<>();

    public BlankModelLayerSectionView() {
        setSpacing(8);
        getChildren().addAll(new Label("Strati del disco (opzionale)"), rowsBox, addLayerBtn);
        addLayerBtn.setOnAction(e -> addLayerRow());
    }

    private void addLayerRow() {
        BlankModelLayerRowView row = new BlankModelLayerRowView(rows.size() + 1);
        rows.add(row);

        row.getRemoveButton().setOnAction(e -> {
            rows.remove(row);
            rowsBox.getChildren().remove(row);
            refreshLayerNumbers();
        });

        rowsBox.getChildren().add(row);
    }

    private void refreshLayerNumbers() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setLayerNumber(i + 1);
        }
    }

    public List<BlankModelLayerDraft> getDrafts() {
        List<BlankModelLayerDraft> drafts = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            drafts.add(rows.get(i).toDraft(i + 1));
        }
        return drafts;
    }
}
