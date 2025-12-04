package com.orodent.tonv2.app;

import com.orodent.tonv2.core.database.Database;
import com.orodent.tonv2.core.csv.CsvPaths;
import com.orodent.tonv2.core.csv.CsvPathsLoader;
import com.orodent.tonv2.core.csv.parser.MagazzinoCsvParser;
import com.orodent.tonv2.features.inventory.database.repository.DepotRepository;
import com.orodent.tonv2.features.inventory.database.repository.ItemRepository;
import com.orodent.tonv2.features.inventory.database.repository.LotRepository;
import com.orodent.tonv2.features.inventory.database.repository.StockRepository;
import com.orodent.tonv2.features.inventory.database.repository.implementation.DepotRepositoryImpl;
import com.orodent.tonv2.features.inventory.database.repository.implementation.ItemRepositoryImpl;
import com.orodent.tonv2.features.inventory.database.repository.implementation.LotRepositoryImpl;
import com.orodent.tonv2.features.inventory.database.repository.implementation.StockRepositoryImpl;

public class AppContainer {

    // --- CSV paths ---
    private final CsvPaths csvPaths;

    // --- Repositories ---
    private final ItemRepository itemRepo;
    private final LotRepository lotRepo;
    private final DepotRepository depotRepo;
    private final StockRepository stockRepo;

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
        System.out.println("Caricati le repository.");

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

    public MagazzinoCsvParser magazzinoParser() { return magazzinoParser; }
}
