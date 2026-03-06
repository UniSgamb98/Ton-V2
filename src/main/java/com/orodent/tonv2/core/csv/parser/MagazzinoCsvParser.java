package com.orodent.tonv2.core.csv.parser;

import com.orodent.tonv2.core.database.repository.DepotRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.core.database.repository.StockRepository;

import java.nio.file.Path;

public class MagazzinoCsvParser implements CsvParser {

    private final ItemRepository itemRepo;
    private final LotRepository lotRepo;
    private final DepotRepository depotRepo;
    private final StockRepository stockRepo;

    public MagazzinoCsvParser(
            ItemRepository itemRepo,
            LotRepository lotRepo,
            DepotRepository depotRepo,
            StockRepository stockRepo
    ) {
        this.itemRepo = itemRepo;
        this.lotRepo = lotRepo;
        this.depotRepo = depotRepo;
        this.stockRepo = stockRepo;
    }

    @Override
    public void parse(Path path) {
        throw new UnsupportedOperationException(
                "Parser magazzino da riallineare al nuovo schema (item con code e flusso production_order)."
        );
    }
}
