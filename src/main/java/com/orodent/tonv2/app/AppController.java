package com.orodent.tonv2.app;

import com.orodent.tonv2.app.assembly.CubageFeatureAssembler;
import com.orodent.tonv2.app.assembly.DocumentsFeatureAssembler;
import com.orodent.tonv2.app.assembly.LaboratoryFeatureAssembler;
import com.orodent.tonv2.app.assembly.RegistersFeatureAssembler;
import com.orodent.tonv2.app.navigation.CubageNavigator;
import com.orodent.tonv2.app.navigation.DocumentsNavigator;
import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.components.AppHeader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;

/** Routes application destinations and owns the lifecycle of the active page. */
public class AppController implements DocumentsNavigator, LaboratoryNavigator, CubageNavigator {
    private final Stage stage;
    private final AppContainer app;
    private final String cssPath;
    private final DocumentsFeatureAssembler documentsAssembler;
    private final LaboratoryFeatureAssembler laboratoryAssembler;
    private final CubageFeatureAssembler cubageAssembler;
    private final RegistersFeatureAssembler registersAssembler;
    private Runnable activePageCleanup = () -> {};

    public AppController(Stage stage, AppContainer app) {
        this.stage = stage;
        this.app = app;
        this.cssPath = Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm();
        this.documentsAssembler = new DocumentsFeatureAssembler(
                app.templateEditorService(), app.connectionProvider(), app.backgroundExecutor());
        this.laboratoryAssembler = new LaboratoryFeatureAssembler(
                app.connectionProvider(), app.templateEditorService(),
                app.documentBrowserService(), app.backgroundExecutor());
        this.cubageAssembler = new CubageFeatureAssembler(app.connectionProvider(), app.backgroundExecutor());
        this.registersAssembler = new RegistersFeatureAssembler(
                app.connectionProvider(), app.templateEditorService(),
                app.documentBrowserService(), app.backgroundExecutor());
        showHome();
    }

    public void showHome() {
        HomeView view = new HomeView();
        showPage(AppPage.immediate(view, view.getHeader(), "TON - Home"));
    }

    @Override
    public void showDocumentsCreate() {
        showPage(documentsAssembler.createTemplate());
    }

    @Override
    public void showDocumentsArchive() {
        showPage(documentsAssembler.archive(this));
    }

    @Override
    public void showDocumentsEditTemplate(int templateId) {
        showPage(documentsAssembler.editTemplate(templateId, this));
    }

    public void showDocuments() {
        showPage(documentsAssembler.home(this));
    }

    public void showCubage() {
        showPage(cubageAssembler.home(this));
    }

    @Override
    public void showCubageCreation() {
        showPage(cubageAssembler.creation());
    }

    @Override
    public void showCubageProductFormulaAssignments() {
        showFeatureNotAvailableAlert();
    }

    @Override
    public void showCubagePayloadContracts() {
        showFeatureNotAvailableAlert();
    }

    public void showRegisters() {
        showPage(registersAssembler.home());
    }

    @Override
    public void showCreateComposition() {
        showPage(laboratoryAssembler.composition(null, this));
    }

    @Override
    public void showCreateComposition(int productId) {
        showPage(laboratoryAssembler.composition(productId, this));
    }

    public void showLaboratory() {
        showPage(laboratoryAssembler.home(this));
    }

    @Override
    public void showBatchProduction() {
        showPage(laboratoryAssembler.batchProduction());
    }

    @Override
    public void showItemSetup() {
        showPage(laboratoryAssembler.itemSetup());
    }

    @Override
    public void showCreateFiringProgram() {
        showPage(laboratoryAssembler.firingProgram(this));
    }

    @Override
    public void showPresintering() {
        showPage(laboratoryAssembler.presintering());
    }

    @Override
    public void showCreateDiskModel() {
        showPage(laboratoryAssembler.diskModel(null, this));
    }

    @Override
    public void showCreateDiskModel(int blankModelId) {
        showPage(laboratoryAssembler.diskModel(blankModelId, this));
    }

    @Override
    public void showLaboratoryCompositionArchive() {
        showPage(laboratoryAssembler.compositionArchive(this));
    }

    @Override
    public void showLaboratoryDiskModelArchive() {
        showPage(laboratoryAssembler.diskModelArchive(this));
    }

    private void showPage(AppPage page) {
        configureHeader(page.header());
        showScene(createSceneWithCSS(page.root()), page.onDispose());
        stage.setTitle(page.title());
        page.onShown().run();
    }

    private Scene createSceneWithCSS(Parent root, String... extraCss) {
        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(cssPath);
        for (String css : extraCss) {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
        }
        return scene;
    }

    private void showScene(Scene scene, Runnable pageCleanup) {
        runActivePageCleanup();
        activePageCleanup = pageCleanup;
        stage.setScene(scene);
    }

    private void runActivePageCleanup() {
        try {
            activePageCleanup.run();
        } catch (RuntimeException exception) {
            System.err.println("Errore durante la chiusura della pagina corrente: " + exception.getMessage());
        }
    }

    private void configureHeader(AppHeader header) {
        header.getHomeButton().setOnAction(event -> showHome());
        header.getLaboratoryButton().setOnAction(event -> showLaboratory());
        header.getCubageButton().setOnAction(event -> showCubage());
        header.getDocumentsButton().setOnAction(event -> showDocuments());
        header.getRegistersButton().setOnAction(event -> showRegisters());
    }

    private void showFeatureNotAvailableAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("TON");
        alert.setHeaderText(null);
        alert.setContentText("Funzionalità non disponibile");
        alert.showAndWait();
    }

    public void shutdown() {
        runActivePageCleanup();
        activePageCleanup = () -> {};
        app.shutdown();
    }
}
