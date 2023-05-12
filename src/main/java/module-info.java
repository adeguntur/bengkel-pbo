module com.bengkel.pbo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.bengkel.pbo to javafx.fxml;
    opens com.bengkel.pbo.model to javafx.base;
    opens com.bengkel.pbo.controller to javafx.fxml;
    exports com.bengkel.pbo;
    exports com.bengkel.pbo.controller;
}
