package com.orodent.tonv2.app;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.concurrent.Executor;

public class StartupController {

    private final StartupView view;
    private final ApplicationInitializer initializer;
    private final Executor executor;
    private final Runnable onReady;
    private final Runnable onClose;
    private Task<Void> activeTask;

    public StartupController(StartupView view,
                             ApplicationInitializer initializer,
                             Executor executor,
                             Runnable onReady,
                             Runnable onClose) {
        this.view = Objects.requireNonNull(view);
        this.initializer = Objects.requireNonNull(initializer);
        this.executor = Objects.requireNonNull(executor);
        this.onReady = Objects.requireNonNull(onReady);
        this.onClose = Objects.requireNonNull(onClose);
        view.getRetryButton().setOnAction(event -> initialize());
        view.getCloseButton().setOnAction(event -> onClose.run());
    }

    public void initialize() {
        if (activeTask != null) {
            return;
        }
        view.showLoading();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                initializer.initialize();
                return null;
            }
        };
        activeTask = task;
        task.setOnSucceeded(event -> {
            activeTask = null;
            onReady.run();
        });
        task.setOnFailed(event -> {
            activeTask = null;
            Throwable error = task.getException();
            view.showError(error == null ? null : error.getMessage());
        });
        task.setOnCancelled(event -> activeTask = null);
        try {
            executor.execute(task);
        } catch (RuntimeException exception) {
            activeTask = null;
            view.showError(exception.getMessage());
        }
    }

    public void dispose() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }
}
