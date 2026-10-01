package com.orodent.tonv2.features.laboratory.presintering.service;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Persists the recoverable presintering draft separately from database work. */
public final class PresinteringLocalPlanStore {
    private final Path planPath;

    public PresinteringLocalPlanStore() {
        this(Path.of(System.getProperty("user.home"), ".ton", "presintering-local-plan.bin"));
    }

    public PresinteringLocalPlanStore(Path planPath) {
        this.planPath = planPath;
    }

    public synchronized Optional<PresinteringLocalPlanState> load() {
        if (!Files.exists(planPath)) {
            return Optional.empty();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(planPath))) {
            Object value = input.readObject();
            if (value instanceof PresinteringLocalPlanState state) {
                return Optional.of(state);
            }
        } catch (Exception ignored) {
            // An unreadable draft must not prevent the page from opening.
        }
        clear();
        return Optional.empty();
    }

    public synchronized void save(PresinteringLocalPlanState state) {
        if (state == null) {
            return;
        }
        try {
            Files.createDirectories(planPath.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(planPath))) {
                output.writeObject(state);
            }
        } catch (Exception ignored) {
            // This is a best-effort recovery file; the database remains authoritative.
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(planPath);
        } catch (Exception ignored) {
            // Failure to remove a recovery file is non-fatal.
        }
    }
}
