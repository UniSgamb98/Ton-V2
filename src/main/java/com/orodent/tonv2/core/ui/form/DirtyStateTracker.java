package com.orodent.tonv2.core.ui.form;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class DirtyStateTracker {

    private final Map<String, Supplier<Object>> stateSuppliers = new LinkedHashMap<>();
    private final Map<String, Object> initialState = new LinkedHashMap<>();

    public DirtyStateTracker track(String key, Supplier<Object> stateSupplier) {
        stateSuppliers.put(key, stateSupplier);
        return this;
    }

    public void captureInitialState() {
        initialState.clear();
        stateSuppliers.forEach((key, supplier) -> initialState.put(key, supplier.get()));
    }

    public boolean hasUnsavedChanges() {
        for (Map.Entry<String, Supplier<Object>> entry : stateSuppliers.entrySet()) {
            if (!Objects.equals(initialState.get(entry.getKey()), entry.getValue().get())) {
                return true;
            }
        }
        return false;
    }
}
