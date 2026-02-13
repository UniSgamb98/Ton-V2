package com.orodent.tonv2.app;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.inventory.controller.InventoryController;
import com.orodent.tonv2.features.inventory.view.InventoryView;
import com.orodent.tonv2.features.laboratory.controller.CreateCompositionController;
import com.orodent.tonv2.features.laboratory.controller.LaboratoryController;
import com.orodent.tonv2.features.laboratory.view.CreateCompositionView;
import com.orodent.tonv2.features.laboratory.view.LaboratoryView;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Objects;


public class AppController {

    /*
    Qua salvo i modelli dell'applicazione e tutte le variabili che servono all'intera applicazione e non alle
    singole pagine.
     */
    private final Stage stage;
    private final AppContainer app;
    private final String cssPath;

    /*
    In questo progetto l'applicazione è state-less. Che significa che tutte le View vengono create da zero sempre.
    Questo significa che i riferimenti dei controller qui sotto non sono necessari. Sarebbero tornati utili nel caso:
    - i controller mantenessero uno stato per ricordare dei filtri impostati dall'utente, il testo inserito in
    un form oppure una selezione di qualche genere.
    - il controller gestisce qualcosa di continuo come Thread, Timer, connessioni TCP. Insomma tutto ciò che ha bisogno
    di essere fermato in un secondo momento oppure se sta leggendo un flusso di dati.
     */

    public AppController(Stage stage) {
        this.stage = stage;
        this.app = new AppContainer();
        this.cssPath = Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm();

        showHome();
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    /*
    -------------------------------------------------------------------------------------------------------------------
    Creo un metodo per ogni view che devo mostrare. Ogni metodo chiama configureHeader per assegnare le funzioni dei
    tasti dello header qua e non nei singoli controller di tutte le view.
     */

    public void showHome() {
        HomeView view = new HomeView();
        configureHeader(view.getHeader());

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Home");
    }

    public void showInventory() {
        InventoryView view = new InventoryView();
        configureHeader(view.getHeader());
        new InventoryController(view, app.itemRepo(), app.depotRepo(), app.stockRepo(), app.lotRepo());

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Inventario");
    }

    public void showCreateComposition() {
        CreateCompositionView view = new CreateCompositionView();
        new CreateCompositionController(view, this, app.powderRepo(), app.compositionRepo(), app.compositionLayerRepo(), app.compositionLayerIngredientRepo(), app.productRepo(), app.itemRepo());
        stage.setScene(createSceneWithCSS(view));
    }


    public void showLaboratory() {
        LaboratoryView view = new LaboratoryView();
        configureHeader(view.getHeader());
        new LaboratoryController(view, this);

        stage.setScene(createSceneWithCSS(view));
        stage.setTitle("TON - Laboratorio");
    }

    /*
    Creando le Scenes con questo metodo vengono collegate al CSS che può essere scritto tutto in un unico file.
     */
    private Scene createSceneWithCSS(Object root) {
        Scene scene = new Scene((javafx.scene.Parent) root, 900, 700);
        scene.getStylesheets().add(cssPath);
        return scene;
    }

    private void configureHeader(AppHeader header) {
        header.getHomeButton().setOnAction(e -> showHome());
        header.getInventoryButton().setOnAction( e -> showInventory());
        header.getLaboratoryButton().setOnAction(e -> showLaboratory());
    }

    public void shutdown() {
        app.database.stop();
    }


    private void ass(){
        app.magazzinoParser().parse(Path.of(app.csvPaths().mod_c()));
    }
}
