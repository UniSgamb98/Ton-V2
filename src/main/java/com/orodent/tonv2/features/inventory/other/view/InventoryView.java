package com.orodent.tonv2.features.inventory.other.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.ui.PopupManager;
import com.orodent.tonv2.features.inventory.database.model.Depot;
import com.orodent.tonv2.features.inventory.database.model.Item;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InventoryView extends VBox {

    private final AppHeader header;

    private final TextField codeField;
    private final TabPane tabPane;
    private final TextField lotField;

    private final Map<String, VBox> itemsBoxes;

    public InventoryView() {
        // Setup
        header = new AppHeader("Inventory");
        tabPane = new TabPane();
        codeField = new TextField();
        lotField = new TextField();

        itemsBoxes = new HashMap<>();

        setSpacing(20);
        setPadding(new Insets(20));

        // Controlli di Selezione
        HBox selectionBox = new HBox(20, codeField, lotField);

        // Layout
        getChildren().addAll(
                header,
                selectionBox,
                tabPane
        );
    }

    //Crea un Tab per ogni Depot presente in Database, Va chiamato da controller.
    public void createTabs(List<Depot> depots) {
        tabPane.getTabs().clear();
        itemsBoxes.clear();

        for (Depot depot : depots) {

            VBox box = new VBox(10);
            itemsBoxes.put(depot.name(), box);

            ScrollPane scroll = new ScrollPane(box);
            scroll.setFitToWidth(true);

            Tab tab = new Tab(depot.name().toUpperCase());
            tab.setClosable(false);
            tab.setContent(scroll);

            tabPane.getTabs().add(tab);
        }
    }

    // Il controller userà questo per mostrare item nel tab giusto
    public void showItems(
            String depotName,
            List<Item> items,
            Map<Integer, Integer> qtyMap,
            Function<Item, Node> popupFactory    // <<--- nuova funzione!
    ) {
        VBox box = itemsBoxes.get(depotName);
        if (box == null) return;

        box.getChildren().clear();

        for (Item item : items) {

            int qty = qtyMap.getOrDefault(item.id(), 0);

            Label lbl = new Label(item.code() + " — " + qty + " pz");
            lbl.getStyleClass().add("inventory-item-row");

            // 🔥 ATTACCA IL POPUP
            Node popupContent = popupFactory.apply(item);
            PopupManager pm = new PopupManager();
            popupManager.attach(label, node -> createPopupForItem(item));

            box.getChildren().add(lbl);
        }
    }

    public TextField getCodeField() {
        return codeField;
    }
    public TextField getLotField() {
        return lotField;
    }
    public AppHeader getHeader() {
        return header;
    }
    public TabPane getTabPane() {
        return tabPane;
    }
}
