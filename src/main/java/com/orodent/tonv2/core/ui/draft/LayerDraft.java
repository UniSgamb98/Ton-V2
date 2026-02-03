package com.orodent.tonv2.core.ui.draft;

import java.util.ArrayList;
import java.util.List;

public class LayerDraft {

    private int layerNumber;
    private final List<IngredientDraft> ingredients = new ArrayList<>();
    private String notes;

    public LayerDraft(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public int layerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public List<IngredientDraft> ingredients() {
        return ingredients;
    }

    public String notes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
