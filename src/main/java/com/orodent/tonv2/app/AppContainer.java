package com.orodent.tonv2.app;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.Database;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AppContainer implements ApplicationInitializer {

    private static final int BACKGROUND_WORKER_COUNT = 3;

    // --- Shared services ---
    private final TemplateEditorService templateEditorService;
    private final DocumentBrowserService documentBrowserService;

    // --- Database ---
    private final Database database;
    private final ConnectionProvider connectionProvider;

    // Migrated operations use one scoped connection per task, so workers can
    // execute independently without sharing a JDBC Connection.
    private final ExecutorService backgroundExecutor;

    protected AppContainer() {

        AtomicInteger workerSequence = new AtomicInteger();
        this.backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_WORKER_COUNT, runnable -> {
            Thread thread = new Thread(runnable, "ton-background-worker-" + workerSequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });

        this.database = new Database();
        this.connectionProvider = database;

        // SHARED SERVICES
        this.templateEditorService = new TemplateEditorService(connectionProvider);
        this.documentBrowserService = new DocumentBrowserService();

    }

    // --- PUBLIC GETTERS ---

    public TemplateEditorService templateEditorService() { return templateEditorService; }
    public DocumentBrowserService documentBrowserService() { return documentBrowserService; }
    public Executor backgroundExecutor() { return backgroundExecutor; }
    public ConnectionProvider connectionProvider() { return connectionProvider; }

    @Override
    public void initialize() {
        database.start();
    }

    @Override
    public void shutdown() {
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println("Alcune operazioni in background non sono terminate entro il timeout.");
            }
        } catch (InterruptedException exception) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        database.stop();
    }
}
