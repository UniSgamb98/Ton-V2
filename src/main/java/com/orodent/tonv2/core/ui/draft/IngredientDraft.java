package com.orodent.tonv2.core.ui.draft;

public class IngredientDraft {

    private int powderId;
    private double percentage;

    public IngredientDraft(int powderId, double percentage) {
        this.powderId = powderId;
        this.percentage = percentage;
    }

    /* ===== GETTER ===== */

    public int powderId() {
        return powderId;
    }

    public double percentage() {
        return percentage;
    }

    /* ===== SETTER ===== */

    public void setPowderId(int powderId) {
        this.powderId = powderId;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
