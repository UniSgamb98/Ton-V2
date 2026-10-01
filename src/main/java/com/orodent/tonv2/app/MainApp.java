package com.orodent.tonv2.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private AppContainer appContainer;
    private AppController appController;
    private StartupController startupController;

    @Override
    public void start(Stage stage) {
        appContainer = new AppContainer();
        StartupView startupView = new StartupView();
        Scene startupScene = new Scene(startupView, 900, 700);
        startupScene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/global.css")).toExternalForm());

        startupController = new StartupController(
                startupView,
                appContainer,
                appContainer.backgroundExecutor(),
                () -> showApplication(stage),
                Platform::exit
        );

        stage.setTitle("TON - Avvio");
        stage.setScene(startupScene);
        stage.show();
        startupController.initialize();
    }

    private void showApplication(Stage stage) {
        startupController.dispose();
        startupController = null;
        appController = new AppController(stage, appContainer);
    }

    @Override
    public void stop() {
        if (startupController != null) {
            startupController.dispose();
        }
        if (appController != null) {
            appController.shutdown();
        } else if (appContainer != null) {
            appContainer.shutdown();
        }
    }
}
