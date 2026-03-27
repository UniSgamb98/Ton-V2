package com.orodent.tonv2.app;

import com.orodent.tonv2.core.database.Database;
import com.orodent.tonv2.core.csv.CsvPaths;
import com.orodent.tonv2.core.csv.CsvPathsLoader;
import com.orodent.tonv2.core.csv.parser.MagazzinoCsvParser;
import com.orodent.tonv2.core.database.implementation.*;
import com.orodent.tonv2.core.database.repository.*;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

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
    private final ProductRepository productRepo;
    private final LineRepository lineRepo;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepo;

    // --- Shared services ---
    private final TemplateEditorService templateEditorService;

    // --- Database ---
    protected final Database database;

    // --- Parsers ---
    private final MagazzinoCsvParser magazzinoParser;

    protected AppContainer() {

        // DATABASE
        this.database = new Database();
        database.start();

        // LOAD CSV PATHS
        this.csvPaths = CsvPathsLoader.load();
        System.out.println("Caricati i path ai csv.");

        // REPOSITORIES
        this.itemRepo = new ItemRepositoryImpl(database.getConnection());
        this.lotRepo = new LotRepositoryImpl(database.getConnection());
        this.depotRepo = new DepotRepositoryImpl(database.getConnection());
        this.stockRepo = new StockRepositoryImpl(database.getConnection());
        this.powderRepo = new PowderRepositoryImpl(database.getConnection());
        this.compositionRepo = new CompositionRepositoryImpl(database.getConnection());
        this.powderOxideRepo = new PowderOxideRepositoryImpl(database.getConnection());
        this.compositionLayerIngredientRepo = new CompositionLayerIngredientRepositoryImpl(database.getConnection());
        this.firingRepo = new FiringRepositoryImpl(database.getConnection());
        this.productionRepo = new ProductionRepositoryImpl(database.getConnection());
        this.productRepo = new ProductRepositoryImpl(database.getConnection());
        this.lineRepo = new LineRepositoryImpl(database.getConnection());
        this.blankModelRepo = new BlankModelRepositoryImpl(database.getConnection());
        this.blankModelLayerRepo = new BlankModelLayerRepositoryImpl(database.getConnection());
        this.blankModelHeightOvermaterialRepo = new BlankModelHeightOvermaterialRepositoryImpl(database.getConnection());
        System.out.println("Caricati le repository.");

        // SHARED SERVICES
        this.templateEditorService = new TemplateEditorService(database::getConnection);

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
    public ProductionRepository productionRepo() { return productionRepo; }
    public ProductRepository productRepo() { return productRepo; }
    public LineRepository lineRepo() { return lineRepo; }
    public BlankModelRepository blankModelRepo() { return blankModelRepo; }
    public BlankModelLayerRepository blankModelLayerRepo() { return blankModelLayerRepo; }
    public BlankModelHeightOvermaterialRepository blankModelHeightOvermaterialRepo() { return blankModelHeightOvermaterialRepo; }

    public TemplateEditorService templateEditorService() { return templateEditorService; }

    public MagazzinoCsvParser magazzinoParser() { return magazzinoParser; }
}
