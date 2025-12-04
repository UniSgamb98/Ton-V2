package com.orodent.tonv2.core.csv.parser;

import com.orodent.tonv2.features.inventory.database.model.Depot;
import com.orodent.tonv2.features.inventory.database.model.Item;
import com.orodent.tonv2.features.inventory.database.model.Lot;
import com.orodent.tonv2.features.inventory.database.repository.DepotRepository;
import com.orodent.tonv2.features.inventory.database.repository.ItemRepository;
import com.orodent.tonv2.features.inventory.database.repository.LotRepository;
import com.orodent.tonv2.features.inventory.database.repository.StockRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MagazzinoCsvParser implements CsvParser {

    private final ItemRepository itemRepo;
    private final LotRepository lotRepo;
    private final DepotRepository depotRepo;
    private final StockRepository stockRepo;

    // Mago4 > user report > magazzino > Articolo_deposito_lotto -> prova.csv

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

        try (BufferedReader reader = Files.newBufferedReader(path)) {

            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] row = line.split(";");

                String itemCode = row[0];
                String lotCode  = row[1];
                int quantity    = Integer.parseInt(row[3]);
                String depotName = row[4];

                // --- DEPOT ---
                Depot depot = depotRepo.findByName(depotName);
                if (depot == null)
                    depot = depotRepo.insert(depotName);

                // --- ITEM ---
                Item item = itemRepo.findByCode(itemCode);
                if (item == null)
                    item = itemRepo.insert(itemCode);

                // --- LOT ---
                Lot lot = lotRepo.findByCodeAndItem(lotCode, item.id());
                if (lot == null)
                    lot = lotRepo.insert(lotCode, item.id());

                // --- STOCK ---
                stockRepo.upsert(lot.id(), depot.id(), quantity);
            }

        } catch (IOException e) {
            throw new RuntimeException("Errore lettura CSV: " + path, e);
        }
    }
}
