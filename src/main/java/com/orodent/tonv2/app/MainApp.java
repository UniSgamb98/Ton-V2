package com.orodent.tonv2.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    AppController appController;

    /*
    AppController è una classe che fa le veci da Router dell'app. Tutti i Controller delle
    varie pagine chiederanno ad AppController di cambiare vista.
     */
    @Override
    public void start(Stage stage) {
        appController = new AppController(stage);
    }

    @Override
    public void stop(){
        appController.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
