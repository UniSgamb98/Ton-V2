package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringLocalPlanState;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringLocalPlanStore;
import javafx.util.Duration;

import java.util.concurrent.Executor;

/** Owns debounce, cancellation and flushing for the local recovery draft. */
final class PresinteringLocalPlanAutosave {
    private final PresinteringLocalPlanStore store;
    private final DebouncedTaskRunner<Void> saveRunner;
    private final DebouncedTaskRunner<Void> clearRunner;
    private PresinteringLocalPlanState pendingState;

    PresinteringLocalPlanAutosave(PresinteringLocalPlanStore store, Executor executor) {
        this.store = store;
        this.saveRunner = new DebouncedTaskRunner<>(executor, Duration.millis(300));
        this.clearRunner = new DebouncedTaskRunner<>(executor, Duration.ZERO);
    }

    void schedule(PresinteringLocalPlanState state) {
        pendingState = state;
        clearRunner.cancel();
        saveRunner.runDebounced(
                () -> {
                    store.save(state);
                    return null;
                },
                () -> {},
                ignored -> {
                    if (pendingState == state) {
                        pendingState = null;
                    }
                },
                ignored -> {}
        );
    }

    void clear(Runnable afterClear) {
        pendingState = null;
        saveRunner.cancel();
        clearRunner.runNow(
                () -> {
                    store.clear();
                    return null;
                },
                () -> {},
                ignored -> run(afterClear),
                ignored -> run(afterClear)
        );
    }

    void dispose() {
        clearRunner.cancel();
        if (pendingState == null) {
            saveRunner.cancel();
            return;
        }
        PresinteringLocalPlanState finalState = pendingState;
        pendingState = null;
        saveRunner.runNow(
                () -> {
                    store.save(finalState);
                    return null;
                },
                () -> {},
                ignored -> {},
                ignored -> {}
        );
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
