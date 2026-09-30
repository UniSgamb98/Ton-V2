package com.orodent.tonv2.core.ui.async;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Runs the latest requested operation away from the JavaFX application thread
 * and delivers its result back on that thread.
 */
public final class DebouncedTaskRunner<T> {

    private final Executor executor;
    private final PauseTransition debounce;

    private Task<T> activeTask;
    private long generation;

    public DebouncedTaskRunner(Executor executor, Duration debounceDuration) {
        this.executor = executor;
        this.debounce = new PauseTransition(debounceDuration);
    }

    public void runNow(Callable<T> operation,
                       Runnable onLoading,
                       Consumer<T> onSuccess,
                       Consumer<Throwable> onFailure) {
        debounce.stop();
        long requestGeneration = prepareLoad(onLoading);
        startTask(requestGeneration, operation, onSuccess, onFailure);
    }

    public void runDebounced(Callable<T> operation,
                             Runnable onLoading,
                             Consumer<T> onSuccess,
                             Consumer<Throwable> onFailure) {
        long requestGeneration = prepareLoad(onLoading);
        debounce.stop();
        debounce.setOnFinished(event -> startTask(
                requestGeneration,
                operation,
                onSuccess,
                onFailure
        ));
        debounce.playFromStart();
    }

    public void cancel() {
        generation++;
        debounce.stop();
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private long prepareLoad(Runnable onLoading) {
        generation++;
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
        onLoading.run();
        return generation;
    }

    private void startTask(long requestGeneration,
                           Callable<T> operation,
                           Consumer<T> onSuccess,
                           Consumer<Throwable> onFailure) {
        if (requestGeneration != generation) {
            return;
        }

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };

        activeTask = task;
        task.setOnSucceeded(event -> {
            if (requestGeneration == generation) {
                activeTask = null;
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (requestGeneration == generation) {
                activeTask = null;
                onFailure.accept(task.getException());
            }
        });
        task.setOnCancelled(event -> {
            if (requestGeneration == generation) {
                activeTask = null;
            }
        });

        try {
            executor.execute(task);
        } catch (RuntimeException exception) {
            if (requestGeneration == generation) {
                activeTask = null;
                onFailure.accept(exception);
            }
        }
    }
}
