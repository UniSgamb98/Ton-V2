package com.orodent.tonv2.features.laboratory.view.partial;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class HeightRangeSectionView extends VBox {

    private final VBox rowsBox = new VBox(8);
    private final Button addRangeBtn = new Button("Aggiungi fascia altezza");
    private final List<HeightRangeRowView> rows = new ArrayList<>();

    public HeightRangeSectionView() {
        setSpacing(8);
        getChildren().addAll(new Label("Overmaterial per fascia altezza (opzionale)"), rowsBox, addRangeBtn);
        addRangeBtn.setOnAction(e -> addRow(null));
    }

    private void addRow(HeightRangeDraft preset) {
        HeightRangeRowView row = new HeightRangeRowView(preset);
        rows.add(row);

        row.getRemoveButton().setOnAction(e -> {
            rows.remove(row);
            rowsBox.getChildren().remove(row);
        });

        rowsBox.getChildren().add(row);
    }

    public List<HeightRangeDraft> getDrafts() {
        List<HeightRangeDraft> drafts = new ArrayList<>();
        for (HeightRangeRowView row : rows) {
            drafts.add(row.toDraft());
        }
        return drafts;
    }
}
