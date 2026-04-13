package com.orodent.tonv2.app;

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

public class AppContainer {

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

    // --- Shared services ---
    private final TemplateEditorService templateEditorService;
    private final DocumentBrowserService documentBrowserService;

    // --- Database ---
    protected final Database database;
    private final Connection sharedConnection;

    // --- Parsers ---
    private final MagazzinoCsvParser magazzinoParser;

    protected AppContainer() {

        // DATABASE
        this.database = new Database();
        database.start();
        this.sharedConnection = database.getConnection();

        // LOAD CSV PATHS
        this.csvPaths = CsvPathsLoader.load();
        System.out.println("Caricati i path ai csv.");

        // REPOSITORIES
        this.itemRepo = new ItemRepositoryImpl(sharedConnection);
        this.lotRepo = new LotRepositoryImpl(sharedConnection);
        this.depotRepo = new DepotRepositoryImpl(sharedConnection);
        this.stockRepo = new StockRepositoryImpl(sharedConnection);
        this.powderRepo = new PowderRepositoryImpl(sharedConnection);
        this.compositionRepo = new CompositionRepositoryImpl(sharedConnection);
        this.powderOxideRepo = new PowderOxideRepositoryImpl(sharedConnection);
        this.compositionLayerIngredientRepo = new CompositionLayerIngredientRepositoryImpl(sharedConnection);
        this.firingRepo = new FiringRepositoryImpl(sharedConnection);
        this.furnaceRepo = new FurnaceRepositoryImpl(sharedConnection);
        this.productionRepo = new ProductionRepositoryImpl(sharedConnection);
        this.productRepo = new ProductRepositoryImpl(sharedConnection);
        this.lineRepo = new LineRepositoryImpl(sharedConnection);
        this.blankModelRepo = new BlankModelRepositoryImpl(sharedConnection);
        this.blankModelLayerRepo = new BlankModelLayerRepositoryImpl(sharedConnection);
        this.blankModelHeightOvermaterialRepo = new BlankModelHeightOvermaterialRepositoryImpl(sharedConnection);
        this.firingProgramRepo = new FiringProgramRepositoryImpl(sharedConnection);
        this.firingProgramRepo.ensureTables();
        System.out.println("Caricati le repository.");

        // SHARED SERVICES
        this.templateEditorService = new TemplateEditorService(() -> sharedConnection);
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

    public TemplateEditorService templateEditorService() { return templateEditorService; }
    public DocumentBrowserService documentBrowserService() { return documentBrowserService; }

    public MagazzinoCsvParser magazzinoParser() { return magazzinoParser; }

    public void shutdown() {
        try {
            if (!sharedConnection.isClosed()) {
                sharedConnection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante la chiusura della connessione condivisa.", e);
        }
    }
}
