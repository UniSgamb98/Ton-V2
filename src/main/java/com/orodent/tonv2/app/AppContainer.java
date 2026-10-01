package com.orodent.tonv2.app;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.Database;
import com.orodent.tonv2.core.database.implementation.*;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AppContainer {

    private static final int BACKGROUND_WORKER_COUNT = 3;

    // --- Repositories ---
    private final ItemRepository itemRepo;
    private final PowderRepository powderRepo;
    private final CompositionRepository compositionRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final LineRepository lineRepo;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;

    // --- Shared services ---
    private final TemplateEditorService templateEditorService;
    private final DocumentBrowserService documentBrowserService;

    // --- Database ---
    protected final Database database;
    // Legacy repositories still use this connection and will be migrated
    // incrementally to the scoped ConnectionProvider.
    private final Connection legacySharedConnection;
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

        // DATABASE
        this.database = new Database();
        database.start();
        this.connectionProvider = database;
        this.legacySharedConnection = database.getConnection();

        // REPOSITORIES
        this.itemRepo = new ItemRepositoryImpl(legacySharedConnection);
        this.powderRepo = new PowderRepositoryImpl(legacySharedConnection);
        this.compositionRepo = new CompositionRepositoryImpl(legacySharedConnection);
        this.compositionLayerIngredientRepo = new CompositionLayerIngredientRepositoryImpl(legacySharedConnection);
        this.lineRepo = new LineRepositoryImpl(legacySharedConnection);
        this.blankModelRepo = new BlankModelRepositoryImpl(legacySharedConnection);
        this.blankModelLayerRepo = new BlankModelLayerRepositoryImpl(legacySharedConnection);
        System.out.println("Caricati le repository.");

        // SHARED SERVICES
        this.templateEditorService = new TemplateEditorService(connectionProvider);
        this.documentBrowserService = new DocumentBrowserService();

    }

    // --- PUBLIC GETTERS ---

    public ItemRepository itemRepo() { return itemRepo; }
    public PowderRepository powderRepo() { return powderRepo; }
    public CompositionRepository compositionRepo() { return compositionRepo; }
    public CompositionLayerIngredientRepository compositionLayerIngredientRepo() { return compositionLayerIngredientRepo; }
    public LineRepository lineRepo() { return lineRepo; }
    public BlankModelRepository blankModelRepo() { return blankModelRepo; }
    public BlankModelLayerRepository blankModelLayerRepo() { return blankModelLayerRepo; }

    public TemplateEditorService templateEditorService() { return templateEditorService; }
    public DocumentBrowserService documentBrowserService() { return documentBrowserService; }
    public Executor backgroundExecutor() { return backgroundExecutor; }
    public ConnectionProvider connectionProvider() { return connectionProvider; }

    public void shutdown() {
        backgroundExecutor.shutdownNow();
        try {
            if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println("Alcune operazioni in background non sono terminate entro il timeout.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        try {
            if (!legacySharedConnection.isClosed()) {
                legacySharedConnection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante la chiusura della connessione condivisa.", e);
        }
    }
}
