package com.orodent.tonv2.features.laboratory.production.presentation;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BatchProductionFormState {

    private Line line;
    private Product product;
    private String templateName;
    private boolean loading;
    private final Map<Item, Integer> quantities = new LinkedHashMap<>();

    public void selectLine(Line line) {
        this.line = line;
        product = null;
        quantities.clear();
    }

    public void selectProduct(Product product) {
        this.product = product;
        quantities.clear();
    }

    public void setItems(List<Item> items) {
        quantities.clear();
        items.forEach(item -> quantities.put(item, 0));
    }

    public void setQuantity(Item item, int quantity) {
        if (quantities.containsKey(item)) {
            quantities.put(item, Math.max(0, quantity));
        }
    }

    public void clearQuantities() {
        quantities.replaceAll((item, quantity) -> 0);
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public List<ItemQuantity> itemQuantities() {
        return quantities.entrySet().stream()
                .map(entry -> new ItemQuantity(entry.getKey(), entry.getValue()))
                .toList();
    }

    public BatchProductionViewState toViewState() {
        int configuredItems = (int) quantities.values().stream().filter(quantity -> quantity > 0).count();
        int totalQuantity = quantities.values().stream().mapToInt(Integer::intValue).sum();
        boolean ready = line != null && product != null && configuredItems > 0;

        String readinessText;
        if (line == null) {
            readinessText = "Seleziona una linea";
        } else if (product == null) {
            readinessText = "Seleziona un prodotto";
        } else if (quantities.isEmpty()) {
            readinessText = "Nessun articolo disponibile";
        } else if (configuredItems == 0) {
            readinessText = "Inserisci almeno una quantità";
        } else {
            readinessText = "Pronto per produrre";
        }

        return new BatchProductionViewState(
                line == null ? "Non selezionata" : line.name(),
                product == null ? "Non selezionato" : product.code(),
                configuredItems,
                totalQuantity,
                templateName == null || templateName.isBlank() ? "Nessun documento" : templateName,
                readinessText,
                ready,
                loading
        );
    }

    public record ItemQuantity(Item item, int quantity) {
    }
}
