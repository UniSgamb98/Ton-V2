module com.orodent.tonv2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.orodent.tonv2 to javafx.fxml;
    exports com.orodent.tonv2;
}