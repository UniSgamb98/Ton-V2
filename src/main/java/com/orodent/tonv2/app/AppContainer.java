package com.orodent.tonv2.app;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.Database;
import com.orodent.tonv2.core.csv.CsvPaths;
import com.orodent.tonv2.core.csv.CsvPathsLoader;
import com.orodent.tonv2.core.csv.parser.MagazzinoCsvParser;
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

    // --- CSV paths ---
    private final CsvPaths csvPaths;

    // --- Repositories ---
    private final ItemRepository itemRepo;
    private final LotRepository lotRepo;
    private final DepotRepository depotRepo;
    private final StockRepository stockRepo;
    private final PowderRepository powderRepo;
    private final PowderOxideRepository powderOxideRepo;
    private final CompositionRepository compositionRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final ProductionRepository productionRepo;
    private final FiringRepository firingRepo;
    private final FurnaceRepository furnaceRepo;
    private final ProductRepository productRepo;
    private final LineRepository lineRepo;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepo;
    private final FiringProgramRepository firingProgramRepo;
    private final PayloadContractRepository payloadContractRepo;
    private final PayloadContractFieldRepository payloadContractFieldRepo;

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

    // --- Parsers ---
    private final MagazzinoCsvParser magazzinoParser;

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

        // LOAD CSV PATHS
        this.csvPaths = CsvPathsLoader.load();
        System.out.println("Caricati i path ai csv.");

        // REPOSITORIES
        this.itemRepo = new ItemRepositoryImpl(legacySharedConnection);
        this.lotRepo = new LotRepositoryImpl(legacySharedConnection);
        this.depotRepo = new DepotRepositoryImpl(legacySharedConnection);
        this.stockRepo = new StockRepositoryImpl(legacySharedConnection);
        this.powderRepo = new PowderRepositoryImpl(legacySharedConnection);
        this.compositionRepo = new CompositionRepositoryImpl(legacySharedConnection);
        this.powderOxideRepo = new PowderOxideRepositoryImpl(legacySharedConnection);
        this.compositionLayerIngredientRepo = new CompositionLayerIngredientRepositoryImpl(legacySharedConnection);
        this.firingRepo = new FiringRepositoryImpl(legacySharedConnection);
        this.furnaceRepo = new FurnaceRepositoryImpl(legacySharedConnection);
        this.productionRepo = new ProductionRepositoryImpl(legacySharedConnection);
        this.productRepo = new ProductRepositoryImpl(legacySharedConnection);
        this.lineRepo = new LineRepositoryImpl(legacySharedConnection);
        this.blankModelRepo = new BlankModelRepositoryImpl(legacySharedConnection);
        this.blankModelLayerRepo = new BlankModelLayerRepositoryImpl(legacySharedConnection);
        this.blankModelHeightOvermaterialRepo = new BlankModelHeightOvermaterialRepositoryImpl(legacySharedConnection);
        this.firingProgramRepo = new FiringProgramRepositoryImpl(legacySharedConnection);
        this.payloadContractRepo = new PayloadContractRepositoryImpl(legacySharedConnection);
        this.payloadContractFieldRepo = new PayloadContractFieldRepositoryImpl(legacySharedConnection);
        System.out.println("Caricati le repository.");

        // SHARED SERVICES
        this.templateEditorService = new TemplateEditorService(connectionProvider);
        this.documentBrowserService = new DocumentBrowserService();

        // PARSER
        this.magazzinoParser = new MagazzinoCsvParser(
                itemRepo,
                lotRepo,
                depotRepo,
                stockRepo
        );
        System.out.println("Caricati i parser.");
    }

    // --- PUBLIC GETTERS ---

    public CsvPaths csvPaths() { return csvPaths; }

    public ItemRepository itemRepo() { return itemRepo; }
    public LotRepository lotRepo() { return lotRepo; }
    public DepotRepository depotRepo() { return depotRepo; }
    public StockRepository stockRepo() { return stockRepo; }
    public PowderRepository powderRepo() { return powderRepo; }
    public CompositionRepository compositionRepo() { return compositionRepo; }
    public CompositionLayerIngredientRepository compositionLayerIngredientRepo() { return compositionLayerIngredientRepo; }
    public PowderOxideRepository powderOxideRepo() { return powderOxideRepo; }
    public FiringRepository firingRepo() { return firingRepo; }
    public FurnaceRepository furnaceRepo() { return furnaceRepo; }
    public ProductionRepository productionRepo() { return productionRepo; }
    public ProductRepository productRepo() { return productRepo; }
    public LineRepository lineRepo() { return lineRepo; }
    public BlankModelRepository blankModelRepo() { return blankModelRepo; }
    public BlankModelLayerRepository blankModelLayerRepo() { return blankModelLayerRepo; }
    public BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepo() { return blankModelHeightOvermaterialRepo; }
    public FiringProgramRepository firingProgramRepo() { return firingProgramRepo; }
    public PayloadContractRepository payloadContractRepo() { return payloadContractRepo; }
    public PayloadContractFieldRepository payloadContractFieldRepo() { return payloadContractFieldRepo; }

    public TemplateEditorService templateEditorService() { return templateEditorService; }
    public DocumentBrowserService documentBrowserService() { return documentBrowserService; }
    public Executor backgroundExecutor() { return backgroundExecutor; }
    public ConnectionProvider connectionProvider() { return connectionProvider; }

    public MagazzinoCsvParser magazzinoParser() { return magazzinoParser; }

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
