package com.bengkel.pbo.controller;

import com.bengkel.pbo.App;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {
    @FXML private Button customerBtn;
    @FXML private Button barangBtn;
    @FXML private Button transaksiBtn;

    @FXML protected void handleButtonAction(javafx.event.ActionEvent event) throws IOException {
        Stage stage;
        Parent root;

        if (event.getSource() == customerBtn) {
            stage = (Stage) customerBtn.getScene().getWindow();
            root = FXMLLoader.load(getClass().getClassLoader().getResource("customer.fxml"));
        } else if (event.getSource() == barangBtn) {
            stage = (Stage) barangBtn.getScene().getWindow();
            root = FXMLLoader.load(getClass().getClassLoader().getResource("barang.fxml"));
        } else {
            stage = (Stage) transaksiBtn.getScene().getWindow();
            root = FXMLLoader.load(getClass().getClassLoader().getResource("transaction.fxml"));
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

