module com.orodent.tonv2 {

    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.apache.derby.server;
    requires org.apache.derby.tools;
    requires java.sql;
    requires java.desktop;

    // Core
    exports com.orodent.tonv2.core.csv;
    exports com.orodent.tonv2.core.csv.parser;
    exports com.orodent.tonv2.core.components;

    // App
    exports com.orodent.tonv2.app;

    // Features
    exports com.orodent.tonv2.features.inventory.database.model;
    exports com.orodent.tonv2.features.inventory.database.repository;
    exports com.orodent.tonv2.core.database;

    opens com.orodent.tonv2.core.csv to com.google.gson;
}
