package com.orodent.tonv2.features.inventory.other.controller;

import com.orodent.tonv2.features.inventory.database.model.Depot;
import com.orodent.tonv2.features.inventory.database.model.Item;
import com.orodent.tonv2.features.inventory.database.repository.DepotRepository;
import com.orodent.tonv2.features.inventory.database.repository.ItemRepository;
import com.orodent.tonv2.features.inventory.database.repository.LotRepository;
import com.orodent.tonv2.features.inventory.database.repository.StockRepository;
import com.orodent.tonv2.features.inventory.other.view.InventoryView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class InventoryController {

    private final InventoryView view;
    private final ItemRepository itemRepo;
    private final DepotRepository depotRepo;
    private final StockRepository stockRepo;
    private final LotRepository lotRepo;
    private Depot currentDepot;

    public InventoryController(InventoryView view, ItemRepository itemRepo, DepotRepository depotRepo, StockRepository stockRepo, LotRepository lotRepo) {
        this.view = view;
        this.itemRepo = itemRepo;
        this.depotRepo = depotRepo;
        this.stockRepo = stockRepo;
        this.lotRepo = lotRepo;

        setupTabs();
        setupListeners();
    }

    private void setupTabs() {
        var depots = depotRepo.findAll();
        view.createTabs(depots);

        // seleziona il primo tab automaticamente
        if (!depots.isEmpty()) {
            loadDepot(depots.getFirst().name());
        }
    }

    private void setupListeners() {
        view.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                String depotName = newTab.getText();
                loadDepot(depotName);
            }
        });
    }

    private Node createPopupForItem(Item item) {
        VBox box = new VBox(6);
        box.getStyleClass().add("popup-content");

        Label title = new Label("Lotti disponibili");
        title.getStyleClass().add("popup-title");
        box.getChildren().add(title);

        var lotti = lotRepo.findByItem(item.id());
        if (lotti == null || lotti.isEmpty()) {
            Label none = new Label("Nessun lotto presente.");
            none.getStyleClass().add("popup-lot-row");
            box.getChildren().add(none);
            return box;
        }

        for (var lot : lotti) {
            // prendi tutti gli stock per questo lotto (può esserci uno stock per ogni deposito)
            var stocks = stockRepo.findByLotAll(lot.id());

            if (stocks == null || stocks.isEmpty()) {
                Label row = new Label(lot.lotCode() + " — qty 0");
                row.getStyleClass().add("popup-lot-row");
                box.getChildren().add(row);
                continue;
            }

            for (var stock : stocks) {
                var depot = depotRepo.findById(stock.depotId()); // depotRepo deve avere findById
                String depotName = depot != null ? depot.name() : "??";
                int qty = stock.quantity(); // oppure stock.qty() a seconda del getter reale

                String text = String.format("%s — qty %d — in %s",
                        lot.lotCode(), qty, depotName);

                Label row = new Label(text);
                row.getStyleClass().add("popup-lot-row");
                box.getChildren().add(row);
            }
        }

        return box;
    }




    private void loadDepot(String depotName) {
        var items = itemRepo.findByDepot(depotName);
        currentDepot = depotRepo.findByName(depotName);

        Map<Integer, Integer> itemQuantities = new HashMap<>();
        for (Item item : items) {
            int qty = stockRepo.getQuantityByItemAndDepot(item.id(), currentDepot.id());
            itemQuantities.put(item.id(), qty);
        }

        view.showItems(
                depotName,
                items,
                itemQuantities,
                this::createPopupForItem
        );
    }
}
